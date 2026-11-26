import Foundation
import Vision

struct RecognizedLine: Encodable {
    let text: String
    let x: Double
    let y: Double
}

guard CommandLine.arguments.count == 2 else {
    FileHandle.standardError.write(Data("Supply one screenshot path.\n".utf8))
    exit(2)
}

let request = VNRecognizeTextRequest()
request.recognitionLevel = .fast
request.usesLanguageCorrection = false
request.recognitionLanguages = ["en-US"]
try VNImageRequestHandler(url: URL(fileURLWithPath: CommandLine.arguments[1])).perform([request])

let lines: [RecognizedLine] = (request.results ?? []).compactMap { observation in
    guard let text = observation.topCandidates(1).first?.string else { return nil }
    let bounds = observation.boundingBox
    return RecognizedLine(text: text, x: Double(bounds.midX), y: Double(1 - bounds.midY))
}
FileHandle.standardOutput.write(try JSONEncoder().encode(lines))
