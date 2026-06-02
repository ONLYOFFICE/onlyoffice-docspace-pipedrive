import React, { useContext, useEffect } from "react";

import { AppContext } from "@context/AppContext";
import { Command } from "@pipedrive/app-extensions-sdk";

import { OnlyofficeSpinner } from "@components/spinner";
import { DealSelector } from "@components/dealSelector/DealSelector";

import { Deal, DealFile } from "src/types/deal";
import { getFileIdFromDownloadUrl } from "@utils/url";
import {
  sendFromDocspaceToPipedrive,
  sendFromPipedriveToDocspace,
} from "@services/files";

const DOCSPACE_URL = "https://aleksandrfedorov.onlyoffice.io";
const DESTINATION_FOLDER_ID = 45930; // TODO: get actual destination folder id

type SelectorMode = "deal" | "file";

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

const FilesPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [selectorMode, setSelectorMode] = React.useState<SelectorMode | null>(
    null,
  );
  const docpaceFileToSend = React.useRef<number | null>(null);

  const { sdk, pipedriveToken } = useContext(AppContext);

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

  const onDownload = (file: string) => {
    docpaceFileToSend.current = getFileIdFromDownloadUrl(file);

    setSelectorMode("deal");
  };

  const handleDealSelect = async (deal: Deal) => {
    try {
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "Sending file to Pipedrive...",
      });

      await sendFromDocspaceToPipedrive(pipedriveToken, {
        targetId: docpaceFileToSend.current!,
        destinationId: deal.id,
      });

      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "File was successfully sent to Pipedrive",
      });
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error("[ONLYOFFICE Files] Failed to send file to Pipedrive", e);

      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "Could not send file to Pipedrive",
      });
    } finally {
      docpaceFileToSend.current = null;
      setSelectorMode(null);
    }
  };

  const handleFileSelect = async (file: DealFile) => {
    try {
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "Uploading file to DocSpace...",
      });

      await sendFromPipedriveToDocspace(pipedriveToken, {
        targetId: file.id,
        destinationId: DESTINATION_FOLDER_ID,
      });

      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "File was successfully uploaded to DocSpace",
      });
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error("[ONLYOFFICE Files] Failed to upload file to DocSpace", e);

      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: "Could not upload file to DocSpace",
      });
    } finally {
      setSelectorMode(null);
    }
  };

  const handleSelectorClose = () => {
    setSelectorMode(null);
    docpaceFileToSend.current = null;
  };

  useEffect(() => {
    const initFiles = () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const docspaceSDK = (window as any).DocSpace?.SDK;
      if (!docspaceSDK) return;

      docspaceSDK.initPersonal({
        frameId: "ds-frame",
        src: DOCSPACE_URL,
        theme: "Base",
        width: "100%",
        height: "100%",
        downloadToEvent: true,
        events: {
          onAppReady,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any, no-console
          onAppError: (e: any) =>
            // eslint-disable-next-line no-console
            console.error("[ONLYOFFICE Files] Docspace SDK - onAppError:", e),
          onDownload,
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
    <div className="w-full h-full flex flex-col relative">
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
      {!loading && (
        <button
          type="button"
          onClick={() => setSelectorMode("file")}
          className="absolute right-4 top-4 z-20 inline-flex h-9 items-center justify-center rounded bg-pipedrive-color-light-blue-600 px-4 text-sm font-semibold text-white shadow hover:bg-pipedrive-color-light-blue-700 focus:outline-none focus:ring-2 focus:ring-pipedrive-color-light-blue-200"
        >
          Import from Pipedrive
        </button>
      )}
      <DealSelector
        isOpen={selectorMode !== null}
        onClose={handleSelectorClose}
        onSelect={handleDealSelect}
        onFileSelect={handleFileSelect}
        pipedriveToken={pipedriveToken}
        mode={selectorMode ?? "deal"}
      />
    </div>
  );
};

export default FilesPage;
