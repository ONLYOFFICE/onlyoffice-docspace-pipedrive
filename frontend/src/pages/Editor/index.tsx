/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import { AppContext, AppErrorType } from "@context/AppContext";
import { Command } from "@pipedrive/app-extensions-sdk";
import { getLocaleForDocspace } from "@utils/locale";
import { ensureDocspaceSdk } from "@utils/docspaceSdk";
import React, { useContext, useEffect, useState } from "react";
import i18next from "i18next";
import { TFrameConfig } from "@onlyoffice/docspace-sdk-js/dist/types/types";
import { getDocspaceAccountToken } from "@services/user";
import { useLocation } from "react-router-dom";
import { OnlyofficeDocspaceLogin } from "@components/docspaceLogin";
import { isPipedriveUnauthorized } from "@utils/error";

const MODAL_WIDTH_PADDING = 64;
const MODAL_HEIGHT_PADDING = 53;

const DOCSPACE_FRAME_ID = "docspace-frame";

const EditorPage: React.FC = () => {
  const { search } = useLocation();
  const { sdk, settings, pipedriveToken, setAppError } = useContext(AppContext);
  const [docspaceAuthorized, setDocspaceAuthorized] = useState(true);

  const data = JSON.parse(new URLSearchParams(search).get("data") || "{}");
  const fileId = data?.fileId || "";
  const mode = data?.mode || "editor";

  useEffect(() => {
    if (!sdk) return;

    sdk.execute(Command.GET_METADATA).then(({ windowWidth, windowHeight }) => {
      sdk.execute(Command.RESIZE, {
        width: windowWidth - MODAL_WIDTH_PADDING,
        height: windowHeight - MODAL_HEIGHT_PADDING,
      });
    });
  }, [sdk]);

  const getToken = async () => {
    try {
      return await getDocspaceAccountToken(pipedriveToken);
    } catch (e) {
      if (isPipedriveUnauthorized(e)) {
        setAppError(AppErrorType.TOKEN_ERROR);
      } else {
        setDocspaceAuthorized(false);
      }
      return "";
    }
  };

  const getEditorDocspaceConfig = (id: string, frameMode: string) => {
    const config = {
      frameId: DOCSPACE_FRAME_ID,
      mode: frameMode,
      width: "100%",
      height: "100%",
      id,
      theme: sdk.userSettings.theme === "dark" ? "Dark" : "Base",
      editorGoBack: false,
      locale: getLocaleForDocspace(i18next.language),
      getToken,
    } as unknown as TFrameConfig;

    return config;
  };

  useEffect(() => {
    if (!settings?.url) {
      return;
    }

    ensureDocspaceSdk(settings.url)
      .then(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const docspaceSDK = (window as any).DocSpace?.SDK;
        if (!docspaceSDK) return;

        const config = getEditorDocspaceConfig(fileId, mode);
        config.src = settings?.url;

        docspaceSDK.init(config);
      })
      .catch((e) =>
        // eslint-disable-next-line no-console
        console.error("[ONLYOFFICE AIChat] Failed to load DocSpace SDK", e),
      );
  }, [settings?.url, fileId, mode, docspaceAuthorized]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      {!docspaceAuthorized && (
        <OnlyofficeDocspaceLogin
          onSuccess={() => setDocspaceAuthorized(true)}
        />
      )}
      {docspaceAuthorized && (
        <div className="w-full h-full flex flex-col items-end">
          <div id={DOCSPACE_FRAME_ID} />
        </div>
      )}
    </>
  );
};
export default EditorPage;
