# ble_exporter

Proof of concept code for streaming metrics from BLE devices.

## Supported devices

* Polar HL7
* SensorTag CC2650
* Muse 2

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

## gRPC

Server up on 0.0.0.0:9002 - see `proto/ble_exporter.proto`

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

## MACs

CC2650: `A4:34:F1:29:BC:71`
Polar H7: `00:22:D0:26:58:91`
Muse 2: `00:55:DA:B5:35:5A`
