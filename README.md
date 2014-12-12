Cordova Local-Notification Plugin
==================================

## Supported Platforms
- **iOS** *(including iOS8)*<br>
- **Android** *(SDK >=11)*<br>
- **WP8**<br>
*Windows Phone 8.0 has no notification center. Instead local notifications are realized through live tiles updates.*


## Dependencies
[Cordova][cordova] will check all dependencies and install them if they are missing.
- [org.apache.cordova.device][apache_device_plugin] *(since v0.6.0)*


# Installation
The plugin can either be installed into the local development environment or cloud based through [PhoneGap Build][PGB].

### Adding the Plugin to your project
Through the [Command-line Interface][CLI]:
```bash
# ~~ from master ~~
cordova plugin add https://github.com/hardikthakkar3/pullNotification.git
```
### Removing the Plugin from your project
Through the [Command-line Interface][CLI]:
```bash
cordova plugin rm com.hardik.thakkar.pullNotification
```
