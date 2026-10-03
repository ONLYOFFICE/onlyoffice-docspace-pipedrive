import React, { useContext, useRef } from "react";

import { AppContext, AppErrorType } from "@context/AppContext";
import { Command, Modal } from "@pipedrive/app-extensions-sdk";
import { DocSpace } from "@onlyoffice/docspace-react";
import {
  SDKInstance,
  SDKMode,
  TEditorOpenPayload,
  TFrameConfig,
} from "@onlyoffice/docspace-sdk-js";

import { OnlyofficeSpinner } from "@components/spinner";
import { DealSelector } from "@components/dealSelector/DealSelector";

import { Deal, DealFile } from "src/types/deal";
import { getFileIdFromDownloadUrl } from "@utils/url";
import {
  sendFromDocspaceToPipedrive,
  sendFromPipedriveToDocspace,
} from "@services/files";
import { getDocspaceAccountToken } from "@services/user";
import { AxiosError } from "axios";
import { ErrorResponse } from "src/types/error";

const DOCSPACE_FRAME_ID = "docspace-frame";
const DESTINATION_FOLDER_ID = 45930; // TODO: get actual destination folder id

type SelectorMode = "deal" | "file";

const FilesPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [selectorMode, setSelectorMode] = React.useState<SelectorMode | null>(
    null,
  );
  const docpaceFileToSend = useRef<number | null>(null);
  const docspaceInstance = useRef<SDKInstance | null>(null);

  const { sdk, settings, pipedriveToken, setAppError } = useContext(AppContext);

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

      docspaceInstance.current?.getFolderInfo().then((folderInfo) => {
        // eslint-disable-next-line no-console
        console.log(
          "[ONLYOFFICE Files] Docspace SDK - folderInfo:",
          folderInfo,
        ); // Wrong method for this mode
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

  const getToken = async () => {
    try {
      return await getDocspaceAccountToken(pipedriveToken);
    } catch (e) {
      const errorData = (e as AxiosError)?.response?.data as ErrorResponse;
      const isDocspaceOAuthError =
        errorData?.cause === "DocspaceOAuth2AuthorizationException";

      if (
        (e as AxiosError)?.response?.status === 401 &&
        !isDocspaceOAuthError
      ) {
        setAppError(AppErrorType.TOKEN_ERROR);
      } else {
        setAppError(AppErrorType.COMMON_ERROR);
      }
      return "";
    }
  };

  const onEditorOpen = async (event: TEditorOpenPayload) => {
    await sdk.execute(Command.OPEN_MODAL, {
      type: Modal.CUSTOM_MODAL,
      action_id: process.env.EDITOR_ACTION_ID || "",
      data: {
        fileId: String(event.id),
        mode: event.action === "edit" ? "editor" : "viewer",
      },
    });
  };

  const getDocspaceConfig = (): TFrameConfig => ({
    frameId: DOCSPACE_FRAME_ID,
    src: settings?.url || "",
    mode: SDKMode.Personal,
    theme: "Base",
    width: "100%",
    height: "100%",
    showMenu: true,
    infoPanelVisible: true,
    downloadToEvent: true,
    getToken,
    events: {
      onAppReady,
      onAppError: (e) =>
        // eslint-disable-next-line no-console
        console.error("[ONLYOFFICE Files] Docspace SDK - onAppError:", e),
      onDownload,
      onEditorOpen,
    },
  });

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
        {settings?.url && (
          <DocSpace
            config={getDocspaceConfig()}
            onSetDocspaceInstance={(instance) => {
              docspaceInstance.current = instance;
            }}
          />
        )}
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
