# ONLYOFFICE DocSpace app for Pipedrive

The ONLYOFFICE DocSpace app for Pipedrive lets you create, share, and co-edit documents inside secure [DocSpace rooms](https://www.onlyoffice.com/docspace) linked to your deals. Each deal gets its own DocSpace room — where deal participants can upload files, edit documents together, and manage access without ever leaving Pipedrive.

<p align="center">
  <a href="https://www.onlyoffice.com/office-for-pipedrive">
    <img src="https://static-site.onlyoffice.com/public/images/templates/office-for-pipedrive/docspace-app/screen1@2x.png" alt="ONLYOFFICE DocSpace for Pipedrive" />
  </a>
</p>

## Key features ✨

- **Room-based collaboration**: Each Pipedrive deal automatically creates a linked DocSpace room for file collaboration.
- **Multiple room types**: Choose from Collaboration, Public, VDR, or Custom rooms depending on the level of privacy and workflow needs.
- **Automatic access sync**: Pipedrive users are synced with corresponding DocSpace groups (e.g., “Pipedrive Users - Company Name”).
- **Granular access control**: Respect Pipedrive's own visibility settings — all users, item owners, or visibility groups.
- **Smart tagging**: Every room is automatically labeled with the tag “Pipedrive Integration” for quick search and organization.
- **Seamless document work**: Upload, view, and co-edit office files directly inside the DocSpace frame.
- **Secure API connection**: Integration uses API keys and permissions for controlled communication between Pipedrive and DocSpace.

## App installation and configuration ⚙️

ONLYOFFICE DocSpace app can be installed via the [Pipedrive Marketplace](https://www.pipedrive.com/en/marketplace/app/onlyoffice-doc-space/4cb3b5d9d19a1918).

> Please note: Each user needs to install the DocSpace app themselves. The Pipedrive admin is not able to install the app for everyone at once.

The Pipedrive admin can configure the app via the **Marketplace apps** section within Pipedrive: Tools and apps -> Marketplace apps -> ONLYOFFICE DocSpace app.

### Connection settings 🔗 (for Pipedrive administrators)

At first, go to your DocSpace ([sign in](https://www.onlyoffice.com/docspace-registration#login)/[sign up](https://www.onlyoffice.com/docspace-registration)). Navigate to the DocSpace **Settings -> Developer Tools -> API keys -> Create new secret key**.

Here, you can assign **Permissions = All** to the API key or customize access by setting **Permissions = Restricted**. Ensure the API key for Pipedrive has at least the following permissions:

* Rooms = Write
* Profile = Read
* Contacts = Write

> Please note: only a user with the DocSpace Admin role is authorized to create the API key.

Once done, go to the ONLYOFFICE DocSpace app settings within Pipedrive, fill in the **DocSpace Service Address** and **ONLYOFFICE DocSpace API Key** fields. Click the Connect button.

If the connection is successful, two buttons will appear on this page:

- **Change**: ability to connect another DocSpace. The data in the current DocSpace will not be deleted.
- **Disconnect**: completely disables the app (clearing user authorization and hooks). In this case, the connection of the Pipedrive Users group with Pipedrive will be removed. We recommend using this option only if there is a need to completely clear data in the DocSpace app.

### Authorization settings 🔑 (for all)

This section is available once the **Connection settings** are configured.

Here, enter your DocSpace credentials (email and password) and click **Login**.

## App usage

The ONLYOFFICE DocSpace app for Pipedrive can be accessed via the Deals section of Pipedrive: go to the corresponding deal and find the DocSpace frame.

For each deal, you can create a separate DocSpace room in which the deal participants can work together on office documents. To create a room, click the **Create room** button and select the appropriate room type (Collaboration/Public/VDR/Custom).

The created room is named according to the rule *Pipedrive - Company name - Deal name*. The room is also labeled with the tag "Pipedrive Integration" for easier identification and organization.

Inside the DocSpace frame, deal participants can work depending on their access rights.

### Access rights 👥

All Pipedrive users who logged into the DocSpace app are added to the **Pipedrive Users (Company Name)** group. If a user logs out of DocSpace, they will be removed from the group.

Access rights to the room within a deal are determined by the Pipedrive access rights to the corresponding deal.

- **All users**: the room becomes available to the Pipedrive Users group as well as the followers are invited by name (only those who have installed the DocSpace app and have been authorized).
- **Item owner**: the room is accessible only to the deal owner and the followers by name. A mandatory condition in this case is that the deal owner and the followers must install the DocSpace app and pass authorization.
- **Item owner's visibility group**: only the followers are synchronized (available in the paid version of Pipedrive).
- **Item owner's visibility group and sub-group**: only the followers are synchronized (available in the paid version of Pipedrive).

## Important to know ❗

If a Pipedrive user does not have access to the room within a deal, they can request access by clicking on the corresponding button. This action checks whether the user should have access to the room. If they should, the user is added to the room based on the Access Rights. If they should not, the corresponding notification is shown.

If the API key becomes invalid (due to expiration or changes in permissions), the DocSpace app will be blocked. Ensure the API key is reviewed and updated as needed.

## Need help or have an idea? 💡

* **✨ Want to know more?** Check out our [product page](https://www.onlyoffice.com/office-for-pipedrive).
* **🐞 Found a bug?** Please report it by creating an [issue](https://github.com/ONLYOFFICE/onlyoffice-docspace-pipedrive/issues).
* **👨‍💻 Need help for developers?** Check our [API documentation](https://api.onlyoffice.com).
* **❓ Have a question?** Ask our community and developers on the [ONLYOFFICE Forum](https://community.onlyoffice.com/).

---
<p align="center">
  Made with ❤️ by the <a href="https://www.onlyoffice.com/">ONLYOFFICE Team</a>
</p>