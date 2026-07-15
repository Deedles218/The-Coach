#include <furi.h>
#include <furi_hal_console.h>
#include <furi_hal_uart.h>
#include <gui/gui.h>
#include <gui/view.h>
#include <gui/view_dispatcher.h>
#include <input/input.h>

#define MAX_NETWORKS 24
#define SSID_LENGTH 33
#define LINE_LENGTH 96

typedef struct {
    char ssid[SSID_LENGTH];
    char security[12];
    int16_t rssi;
    uint8_t channel;
} WifiNetwork;

typedef struct {
    WifiNetwork networks[MAX_NETWORKS];
    uint8_t count;
    uint8_t selected;
    bool scanning;
    char status[24];
} WifiAuditorModel;

typedef struct {
    Gui* gui;
    ViewDispatcher* dispatcher;
    View* view;
    FuriThread* worker;
    FuriStreamBuffer* rx_stream;
    char line[LINE_LENGTH];
    size_t line_length;
} WifiAuditorApp;

typedef enum {
    WorkerEventStop = (1 << 1),
    WorkerEventRx = (1 << 2),
} WorkerEvent;

static const char* security_rating(const char* security) {
    if(strcmp(security, "OPEN") == 0 || strcmp(security, "WEP") == 0) return "RISK";
    if(strcmp(security, "WPA") == 0 || strcmp(security, "UNKNOWN") == 0) return "WARN";
    return "OK";
}

static void wifi_auditor_draw(Canvas* canvas, void* context) {
    WifiAuditorModel* model = context;
    canvas_clear(canvas);
    canvas_set_font(canvas, FontPrimary);
    canvas_draw_str(canvas, 2, 10, "WiFi Auditor");
    canvas_set_font(canvas, FontSecondary);

    if(model->scanning || model->count == 0) {
        canvas_draw_str(canvas, 2, 29, model->status);
        canvas_draw_str(canvas, 2, 62, "OK: scan   Back: exit");
        return;
    }

    uint8_t first = model->selected > 1 ? model->selected - 1 : 0;
    for(uint8_t row = 0; row < 3 && first + row < model->count; row++) {
        uint8_t index = first + row;
        WifiNetwork* network = &model->networks[index];
        char text[48];
        snprintf(
            text,
            sizeof(text),
            "%c%-15.15s %s",
            index == model->selected ? '>' : ' ',
            network->ssid[0] ? network->ssid : "<hidden>",
            security_rating(network->security));
        canvas_draw_str(canvas, 0, 22 + row * 12, text);
    }

    WifiNetwork* selected = &model->networks[model->selected];
    char footer[48];
    snprintf(
        footer,
        sizeof(footer),
        "CH%u %ddBm %s | OK:scan",
        selected->channel,
        selected->rssi,
        selected->security);
    canvas_draw_str(canvas, 0, 63, footer);
}

static void wifi_auditor_request_scan(WifiAuditorApp* app) {
    static uint8_t command[] = "SCAN\n";
    with_view_model(
        app->view,
        WifiAuditorModel * model,
        {
            model->count = 0;
            model->selected = 0;
            model->scanning = true;
            strlcpy(model->status, "Scanning...", sizeof(model->status));
        },
        true);
    furi_hal_uart_tx(FuriHalUartIdUSART1, command, sizeof(command) - 1);
}

static bool wifi_auditor_input(InputEvent* event, void* context) {
    WifiAuditorApp* app = context;
    if(event->type != InputTypeShort && event->type != InputTypeRepeat) return false;

    if(event->key == InputKeyOk) {
        wifi_auditor_request_scan(app);
        return true;
    }

    with_view_model(
        app->view,
        WifiAuditorModel * model,
        {
            if(event->key == InputKeyUp && model->selected > 0) {
                model->selected--;
            } else if(event->key == InputKeyDown && model->selected + 1 < model->count) {
                model->selected++;
            }
        },
        true);
    return event->key == InputKeyUp || event->key == InputKeyDown;
}

static uint32_t wifi_auditor_exit(void* context) {
    UNUSED(context);
    return VIEW_NONE;
}

static void wifi_auditor_parse_line(WifiAuditorApp* app) {
    app->line[app->line_length] = '\0';
    if(strcmp(app->line, "BEGIN") == 0) {
        with_view_model(
            app->view,
            WifiAuditorModel * model,
            {
                model->count = 0;
                model->selected = 0;
                model->scanning = true;
                strlcpy(model->status, "Receiving...", sizeof(model->status));
            },
            true);
    } else if(strcmp(app->line, "END") == 0) {
        with_view_model(
            app->view,
            WifiAuditorModel * model,
            {
                model->scanning = false;
                snprintf(model->status, sizeof(model->status), "%u networks", model->count);
            },
            true);
    } else if(strncmp(app->line, "NET\t", 4) == 0) {
        char* ssid = app->line + 4;
        char* channel = strchr(ssid, '\t');
        char* rssi = channel ? strchr(channel + 1, '\t') : NULL;
        char* security = rssi ? strchr(rssi + 1, '\t') : NULL;
        if(ssid && channel && rssi && security) {
            *channel++ = '\0';
            *rssi++ = '\0';
            *security++ = '\0';
            with_view_model(
                app->view,
                WifiAuditorModel * model,
                {
                    if(model->count < MAX_NETWORKS) {
                        WifiNetwork* network = &model->networks[model->count++];
                        strlcpy(network->ssid, ssid, sizeof(network->ssid));
                        strlcpy(network->security, security, sizeof(network->security));
                        network->channel = atoi(channel);
                        network->rssi = atoi(rssi);
                    }
                },
                true);
        }
    } else if(strncmp(app->line, "ERROR\t", 6) == 0) {
        with_view_model(
            app->view,
            WifiAuditorModel * model,
            {
                model->scanning = false;
                strlcpy(model->status, app->line + 6, sizeof(model->status));
            },
            true);
    }
    app->line_length = 0;
}

static void wifi_auditor_uart_callback(UartIrqEvent event, uint8_t data, void* context) {
    WifiAuditorApp* app = context;
    if(event == UartIrqEventRXNE) {
        furi_stream_buffer_send(app->rx_stream, &data, 1, 0);
        furi_thread_flags_set(furi_thread_get_id(app->worker), WorkerEventRx);
    }
}

static int32_t wifi_auditor_worker(void* context) {
    WifiAuditorApp* app = context;
    while(true) {
        uint32_t events = furi_thread_flags_wait(
            WorkerEventStop | WorkerEventRx, FuriFlagWaitAny, FuriWaitForever);
        if(events & WorkerEventStop) break;
        if(events & WorkerEventRx) {
            uint8_t data[64];
            size_t received;
            while((received = furi_stream_buffer_receive(app->rx_stream, data, sizeof(data), 0))) {
                for(size_t i = 0; i < received; i++) {
                    if(data[i] == '\n') {
                        wifi_auditor_parse_line(app);
                    } else if(data[i] != '\r' && app->line_length < LINE_LENGTH - 1) {
                        app->line[app->line_length++] = data[i];
                    }
                }
            }
        }
    }
    return 0;
}

int32_t wifi_auditor_app(void* context) {
    UNUSED(context);
    WifiAuditorApp* app = malloc(sizeof(WifiAuditorApp));
    memset(app, 0, sizeof(WifiAuditorApp));

    app->rx_stream = furi_stream_buffer_alloc(1024, 1);
    app->gui = furi_record_open(RECORD_GUI);
    app->dispatcher = view_dispatcher_alloc();
    app->view = view_alloc();
    view_allocate_model(app->view, ViewModelTypeLocking, sizeof(WifiAuditorModel));
    view_set_context(app->view, app);
    view_set_draw_callback(app->view, wifi_auditor_draw);
    view_set_input_callback(app->view, wifi_auditor_input);
    view_set_previous_callback(app->view, wifi_auditor_exit);
    view_dispatcher_enable_queue(app->dispatcher);
    view_dispatcher_attach_to_gui(app->dispatcher, app->gui, ViewDispatcherTypeFullscreen);
    view_dispatcher_add_view(app->dispatcher, 0, app->view);
    view_dispatcher_switch_to_view(app->dispatcher, 0);

    with_view_model(
        app->view,
        WifiAuditorModel * model,
        {
            memset(model, 0, sizeof(WifiAuditorModel));
            strlcpy(model->status, "OK to scan", sizeof(model->status));
        },
        true);

    app->worker = furi_thread_alloc_ex("WifiAuditRx", 2048, wifi_auditor_worker, app);
    furi_thread_start(app->worker);
    furi_hal_console_disable();
    furi_hal_uart_set_br(FuriHalUartIdUSART1, 115200);
    furi_hal_uart_set_irq_cb(FuriHalUartIdUSART1, wifi_auditor_uart_callback, app);

    wifi_auditor_request_scan(app);
    view_dispatcher_run(app->dispatcher);

    furi_hal_console_enable();
    furi_thread_flags_set(furi_thread_get_id(app->worker), WorkerEventStop);
    furi_thread_join(app->worker);
    furi_thread_free(app->worker);
    view_dispatcher_remove_view(app->dispatcher, 0);
    view_free(app->view);
    view_dispatcher_free(app->dispatcher);
    furi_record_close(RECORD_GUI);
    furi_stream_buffer_free(app->rx_stream);
    free(app);
    return 0;
}
