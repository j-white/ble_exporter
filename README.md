# sensortag_exporter

```
mvn install:install-file \
   -Dfile=lib/tinyb.jar \
   -DgroupId=tinyb \
   -DartifactId=tinyb \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

See https://forum.snapcraft.io/t/using-bluetooth-w-rpi4-on-uc18/16975
```
sudo snap refresh bluez --channel=edge --devmode
sudo hciattach /dev/ttyAMA0 bcm43xx 921600 noflow -
```

Bluetooth on RPI4
```
sudo snap install sensortag_exporter_1_arm64.snap  --devmode --dangerous
sudo snap connect sensortag_exporter:bluetooth-control
sudo snap connect sensortag_exporter:bluez bluez
```

```
sudo sensortag_exporter 00:55:DA:B5:35:5A
```

