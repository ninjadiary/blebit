# BLE:Bit Tool

## Who

The BLE:Bit tool is developed and maintained by [Theodoros Danos.](https://twitter.com/fand0mas) 

## What

BLE:Bit is an open source tool to assist in Bluetooth Low Energy security auditing. Also, it is a tool that can be used in a development project, while it permits a connection to any BLE device and helps to exchange data in GATT layer.

## Why

There are multiple BLE-related projects that enables to do pretty much everything, made by remarkable people. Even though such tools exist, BLE:Bit took a very different approach which allows room for more options and tweaks regarding the user configurations. BLE:Bit makes the job easy. It is easy create a program to connect to any device, and provides many connection-based configurations, PIN-based configurations and enough callbacks with the appropriate BLE-standard error codes. With the appropriate errors one can troubleshoot and debug BLE-based devices in a more straightforward way. Some tools are provided, but the BLE:Bit tool is mostly built as a tool for finding vulnerabilities by creating tools-per-need, rather than to be used as "execute allthethings". Also, it makes the entrance to BLE-world very easy even for BLE-rookies.

## How

BLE:Bit is divided in two units: Central and Peripheral

Central, works as a "client", as it helps to connect to other BLE:Bit devices.

Peripheral, works as a "server" when other devices connect to the device, offering various services defined by the user

Multiple tools have been developed by the author, but you are free to use the BLE:Bit SDK provided in this repository to make your own tools. The tools developed are only Proof-of-Concept programs of what actually the SDK can do. The full potential of the tool lies on the SDK and not on the tools.

The BLE:Bit tool is consisted by both software and hardware. Regarding the hardware, schematics and firmware, can be found in "hw" directory, while the software is hosted in "src" directory.

If you wish to receive a BLE:Bit hardware with the latest firmware, it can be purchased at [shellwanted.com](https://shop.shellwanted.com/), even though i dare you to create one by yourself.

**For SDK documentation & examples visit**: [docs.blebit.io](https://docs.blebit.io)

**Case Study Projects** @ [shellwanted.com](https://shellwanted.com)

## MIT License

Copyright (c) 2022 Theodoros Danos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
