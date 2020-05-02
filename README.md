# ble_exporter

## Supported devices

* Polar HL7
* SensorTag CC2650

## Building from source

```
mvn install:install-file \
   -Dfile=lib/tinyb.jar \
   -DgroupId=tinyb \
   -DartifactId=tinyb \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

## Installing snap

See https://forum.snapcraft.io/t/using-bluetooth-w-rpi4-on-uc18/16975
```
sudo snap refresh bluez --channel=edge --devmode
sudo hciattach /dev/ttyAMA0 bcm43xx 921600 noflow -
```

Bluetooth on RPI4
```
sudo snap install ble_exporter_1_arm64.snap  --devmode --dangerous
sudo snap connect ble_exporter:bluetooth-control
sudo snap connect ble_exporter:bluez bluez
```

```
sudo ble_exporter A4:34:F1:29:BC:71
sudo ble_exporter 00:22:D0:26:58:91
```

