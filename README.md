# The Coach: Men's Health tracker
 The Coach life style app Test Automation Project. Tests for iOS platform. 
Java/Appium/Maven/Jenkins/Allure

## Run iOS tests for a specific build

The bundle ID can be supplied either as the `ios.bundleId` Maven property or
as the `IOS_BUNDLE_ID` environment variable. The Maven property takes
precedence over the environment variable.

```bash
mvn test -Dplatform=ios -Dios.bundleId=com.vamapps.preprod.The-Coach
```

```bash
IOS_BUNDLE_ID=com.vamapps.The-Coach mvn test -Dplatform=ios
```

Known bundle IDs:

- `com.vamapps.preprod.The-Coach`
- `com.vamapps.The-Coach`
- `com.vamapps.The-Coach-for-her`

If neither option is supplied, `com.vamapps.preprod.The-Coach` is used.
