import React, { useContext, useRef } from "react";

import { AppContext, AppErrorType } from "@context/AppContext";
import { Command, Modal } from "@pipedrive/app-extensions-sdk";
import { DocSpace } from "@onlyoffice/docspace-react";
import {
  SDKInstance,
  SDKMode,
  TCustomActionEvent,
  TEditorOpenPayload,
  TFrameConfig,
} from "@onlyoffice/docspace-sdk-js";

import { OnlyofficeSpinner } from "@components/spinner";
import { DealSelector } from "@components/dealSelector/DealSelector";

import { Deal, DealFile } from "src/types/deal";
import {
  sendFromDocspaceToPipedrive,
  sendFromPipedriveToDocspace,
} from "@services/files";
import { getDocspaceAccountToken } from "@services/user";
import { AxiosError } from "axios";
import { ErrorResponse } from "src/types/error";

const DOCSPACE_FRAME_ID = "docspace-frame";
const SEND_TO_PIPEDRIVE_ACTION = "send-to-pipedrive";
const IMPORT_FROM_PIPEDRIVE_ACTION = "import-from-pipedrive";

type SelectorMode = "deal" | "file";

const FilesPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [selectorMode, setSelectorMode] = React.useState<SelectorMode | null>(
    null,
  );
  const docpaceFileToSend = useRef<number | null>(null);
  const docspaceDestinationFolder = useRef<number | null>(null);
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

    docspaceInstance.current?.setCustomActions({
      contextMenu: {
        file: [
          {
            key: SEND_TO_PIPEDRIVE_ACTION,
            label: "Send to Pipedrive",
            requireSecurity: ["Download"],
          },
        ],
      },
      createMenu: [
        {
          key: IMPORT_FROM_PIPEDRIVE_ACTION,
          label: "Import from Pipedrive",
        },
      ],
    });
  };

  const onCustomAction = ({
    action,
    type,
    item,
    items,
    folderId,
  }: TCustomActionEvent) => {
    if (action === IMPORT_FROM_PIPEDRIVE_ACTION && type === "create") {
      if (folderId === undefined) return;

      docspaceDestinationFolder.current = Number(folderId);

      setSelectorMode("file");
      return;
    }

    if (action !== SEND_TO_PIPEDRIVE_ACTION || type !== "file") return;

    const file = (item ?? items?.[0]) as { id: number } | undefined;
    if (!file) return;

    docpaceFileToSend.current = file.id;

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
        destinationId: docspaceDestinationFolder.current!,
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
      docspaceDestinationFolder.current = null;
      setSelectorMode(null);
    }
  };

  const handleSelectorClose = () => {
    setSelectorMode(null);
    docpaceFileToSend.current = null;
    docspaceDestinationFolder.current = null;
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
    getToken,
    events: {
      onAppReady,
      onAppError: (e) =>
        // eslint-disable-next-line no-console
        console.error("[ONLYOFFICE Files] Docspace SDK - onAppError:", e),
      onCustomAction,
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
