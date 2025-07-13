### Synchronize
Assume scenario where there are two devices with this app installed:
a) sender
b) receiver

When user of "sender" device clicks synchronize buton they should be presented with list of available receivers to connect to. The receivers are available bluetooth devices that have this app installed. User should be able to select one of the receivers and connect to it. Let's assume the device chosen by user is the "receiver" device.

The sender device should then send a list of all categories and products to the receiver device. The receiver device should then look up it's own list of categories and products and add any missing categories, and compare local list of products with the list received from the sender device. If a product is missing in "receiver" device, it should be added. If it exists in "receiver" device, a state with latest lastModified value takes precedence. So, if lastModified is bigger on "receiver" device, the state on "receiver" device should be left unchanged, otherwise value of lastModified and quantity from "sender" device should be used to update the state on "receiver" device. 

Afterwards the "receiver" device should send a list of all categories and products to the "sender" device. The "sender" device should then look up it's own list of categories and products and add any missing categories, and compare local list of products with the list received from the "receiver" device. If a product is missing in "sender" device, it should be added. If it exists in "sender" device, a state with latest lastModified value takes precedence. So, if lastModified is bigger on "sender" device, the state on "sender" device should be left unchanged, otherwise value of lastModified and quantity from "receiver" device should be used to update the state on "sender" device. 

Lastly a dialog should be presented to the user of "sender" device with information that the synchronization is complete. The dialog should have a button to close it. 

