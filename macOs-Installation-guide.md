**NOTE: Currently supported browsers: ONLY Firefox and Chromium, works on Habbo AIR too**

In order to run G-Earth on macOs, you'll need to sign G-Mem (our memory searcher). This wiki page will cover that process.

First we'll create a certificate in order to sign it:
1. Open the Keychain Access application (You may find it inside Utilities).£
2. Select Certificate Assistant -> Create a Certificate.<br>
![](https://i.imgur.com/G6SS6ac.png)
3. Choose a name for the certificate (I'll use "gmem-cert") and set "Certificate Type" to "Code Signing". Also select "Let me override defaults" option.<br>
![](https://i.imgur.com/CAUI5Xi.png)
4. Click on "Continue" until "Specify a Location For The Certificate" appears, then set "Keychain" to "System".<br>
![](https://i.imgur.com/HwLDtmE.png)
5. Continue, the certificate will be created then.<br>
![](https://i.imgur.com/gYiKmZA.png)

Once created, we are able to codesign gmem from Terminal<br>
`codesign -fs "gmem-cert" <G-Earth_Path>/G-Mem`

![](https://i.imgur.com/xkryoJz.png)

Now you're ready to open G-Earth from Terminal<br>
`sudo java -jar G-Earth.jar`

If you experience any other issues and the troubleshooting page doesn't help, it might be useful to have a look at the following issues: [#67](../issues/67) [#10](../issues/10)