# WifiIndoorLocation
Android application that is able to localize walking people without directly relying on gps. This is particularly useful in indoor environments.
The application uses two different localization methodologies:
* **wifi fingerprinting**: a range free localization technique able to estimate the current user absolute position, inferring it from the wifi access-points constellation sensed in the current user position;
* **inertial navigation**: through pedometer and rotation sensor the instantaneous user direction vector can be estimated.

The real time estimated location is visualized on a map (Google Map APIs). Through a calibration process, the location can be estimated even if the phone is kept in the pocket.

The application needs an offline phase, a "learning phase" in which the wifi fingerprint for a certain location is recorded and associated to the actual geographical coordinates (manually inserted or acquired by means of GPS).

**Note**: this is not a fully reliable navigation application (it is affected by magnetic noise and wifi precision depends on the surrounding environment, however usually lower than 10 meters). It is intended to be an academic work for implementing and studying the above specified techniques.

For additional informations, see **presentation.pdf**
