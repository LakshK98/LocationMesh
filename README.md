# LocationMesh
 A calamity relief app that collects location information of all victims without network connectivity. The app will maintain a table containing name and location of itself as well as all the discovered peers.It will then use wifi P2P android API to get connected to its
peers . Once it gets connected to a peer these tables will be exchanged , both
devices will append any name from the table which it does not already have. This
will create a mesh network that will give location of all victims in a calamity
struck region where there is no mobile network. 

## Impementation details:
● The app starts peer discovery every 15 seconds because otherwise the wifi p2p module stops.
● It then tries to connect to all available peers in its peer list after waiting for a random duration between 5 to 6 seconds
between each connection.
● Once two devices are connected they exchange their entire location table.

## User manual:
● All users in an area need to launch the app and press the
start service button.
● The app will start transferring data among all users who are
using the app.
● Whenever a user wants to stop exchanging fdata he/she
needs to press the same button again.
