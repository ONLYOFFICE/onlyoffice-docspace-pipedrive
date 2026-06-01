import React, { useContext, useEffect } from "react";

import { AppContext } from "@context/AppContext";
import { Command } from "@pipedrive/app-extensions-sdk";
import { SDKInstance } from "@onlyoffice/docspace-sdk-js/dist/types/instance";

import { OnlyofficeSpinner } from "@components/spinner";

const DOCSPACE_URL = "https://aleksandrfedorov.onlyoffice.io";

let sdkLoaded = false;
let sdkLoading: Promise<void> | null = null;

function ensureSdk(): Promise<void> {
  if (sdkLoaded) return Promise.resolve();
  if (sdkLoading) return sdkLoading;

  sdkLoading = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = `${DOCSPACE_URL}/static/scripts/sdk/2.2.0/api.js`;
    script.onload = () => {
      sdkLoaded = true;
      sdkLoading = null;
      resolve();
    };
    script.onerror = (e) => {
      sdkLoading = null;
      reject(e);
    };
    document.head.appendChild(script);
  });

  return sdkLoading;
}

let instance: SDKInstance | null = null;

const FilesPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);

  const { sdk } = useContext(AppContext);

  sdk.execute(Command.RESIZE, {
    width: 800,
    height: 680,
  });

  const onAppReady = () => {
    // eslint-disable-next-line no-console
    console.log("[ONLYOFFICE Files] Docspace SDK - onAppReady");
    setLoading(false);

    // const fileActions = [
    //   {
    //     key: "send-to-nextcloud",
    //     label: "Send to Pipedrive",
    //     icon: "",
    //   },
    // ];

    // instance.setCustomActions({ contextMenu: { file: fileActions } }); // TODO: add context menu actions when supported by the SDK
  };

  useEffect(() => {
    const initFiles = () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const docspaceSDK = (window as any).DocSpace?.SDK;
      if (!docspaceSDK) return;

      instance = docspaceSDK.initPersonal({
        frameId: "ds-frame",
        src: DOCSPACE_URL,
        theme: "Base",
        width: "100%",
        height: "100%",
        events: {
          onAppReady,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any, no-console
          onAppError: (e: any) =>
            // eslint-disable-next-line no-console
            console.error("[ONLYOFFICE Files] Docspace SDK - onAppError:", e),
        },
      });
    };

    ensureSdk()
      .then(() => {
        initFiles();
      })
      .catch((e) =>
        // eslint-disable-next-line no-console
        console.error("[ONLYOFFICE Files] Failed to load DocSpace SDK", e),
      );
  }, []);

  return (
    <div className="w-full h-full flex flex-col">
      {loading && (
        <div className="h-full w-full flex justify-center items-center">
          <OnlyofficeSpinner />
        </div>
      )}
      <div
        className={`w-full h-full flex flex-col items-end ${loading ? "hidden" : ""}`}
      >
        <div id="ds-frame" />
      </div>
    </div>
  );
};

export default FilesPage;
