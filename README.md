# ble_exporter

Proof of concept code for streaming metrics from BLE devices.

## Supported devices

* Polar HL7
* SensorTag CC2650
* Muse 2

## Building from source

### Optionally install dependencies

Install dependencies:
```
sudo apt -y install git openjdk-8-jdk
```

Install maven:
```
wget https://mirror.its.dal.ca/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
sudo tar xf apache-maven-*.tar.gz -C /opt
sudo mv /opt/apache-maven-* /opt/maven
echo 'export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=${M2_HOME}/bin:${PATH}' | sudo tee -a /etc/profile.d/maven.sh
sudo chmod +x /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
```
### Build 

Clone the project:
```
git clone https://github.com/j-white/ble_exporter.git
```

Add `tinyb` to the local Maven repository:
```
mvn install:install-file \
   -Dfile=lib/tinyb.jar \
   -DgroupId=tinyb \
   -DartifactId=tinyb \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DgeneratePom=true
```

Build the package with:
```
mvn clean package
```

## Running

```
java -jar target/ble_exporter-1.0.0-SNAPSHOT.jar 00:22:D0:26:58:91
```

## gRPC

Server up on 0.0.0.0:9002 - see `proto/ble_exporter.proto`

## Testing on a RPi4 w/ Ubuntu Server 18.04

Flash RPi4 w/ `ubuntu-18.04.4-preinstalled-server-arm64+raspi4.img`.

Update the system:
```
sudo apt update && sudo apt upgrade -y
```

Enable Bluetooth:
* Edit `/boot/firmware/config.txt` and change `cmdline=nobtcmd.txt` to `cmdline=btcmd.txt`
* Edit `/boot/firmware/syscfg.txt` and change `include nobtcfg.txt` to `include btcfg.txt`

Install bluez:
```
sudo apt install -y bluetooth
sudo systemctl enable bluetooth
```

Install the pi-bluetooth package:
```
wget http://turul.canonical.com/ubuntu-ports/pool/multiverse/p/pi-bluetooth/pi-bluetooth_0.1.10ubuntu6_arm64.deb
sudo dpkg -i pi-bluetooth_0.1.10ubuntu6_arm64.deb
```

> Thanks for https://tomaraei.com/blog/how-to-enable-bluetooth-on-raspberry-pi-4-ubuntu-server-18-04 for the hint :)

Give the user access:
```
sudo usermod -G bluetooth -a ubuntu
```

Reboot the device.

The Bluetooth controller should now be accessible:
```
ubuntu@ubuntu:~$ sudo bluetoothctl
ubuntu@ubuntu:~$ bluetoothctl 
[NEW] Controller B8:27:EB:D0:C1:98 ubuntu [default]
Agent registered
[bluetooth]# list
Controller B8:27:EB:D0:C1:98 ubuntu [default]
```

## Testing on RPi4 w/ Ubuntu Core 18 & Snap

See https://forum.snapcraft.io/t/using-bluetooth-w-rpi4-on-uc18/16975

Install `bluez` from the `edge` channel:
```
sudo snap install bluez --channel=edge --devmode
```

Create the adapter (may need to run this multiple times):
```
sudo hciattach /dev/ttyAMA0 bcm43xx 921600 noflow -
```

After success:
```
ubuntu@localhost:~$ sudo hciattach /dev/ttyAMA0 bcm43xx 921600 noflow -
bcm43xx_init
Flash firmware /lib/firmware/brcm/BCM4345C0.hcd
Set Controller UART speed to 921600 bit/s
Device setup complete
```

`bluetoothctl` should work now:
```
ubuntu@localhost:~$ sudo bluez.bluetoothctl 
[NEW] Controller DC:A6:32:1A:60:26 BlueZ 5.47 [default]
Agent registered
[bluetooth]# 
```

Install the snap:
```
sudo snap install ble-exporter_1_arm64.snap --devmode --dangerous
sudo snap connect ble-exporter:bluetooth-control
sudo snap connect ble-exporter:bluez bluez
```

## MACs

CC2650: `A4:34:F1:29:BC:71`
Polar H7: `00:22:D0:26:58:91`
Muse 2: `00:55:DA:B5:35:5A`

