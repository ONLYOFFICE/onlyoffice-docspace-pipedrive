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
import React, { useContext, useEffect, useState } from "react";
import i18next from "i18next";
import type {
  TAuthError,
  TFrameConfig,
  TFrameEvents,
} from "@onlyoffice/docspace-sdk-js";
import { DocSpace } from "@onlyoffice/docspace-react";
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

  const onAuthError = (error: TAuthError) => {
    // eslint-disable-next-line no-console
    console.error(error);

    setDocspaceAuthorized(false);
  };

  const getToken = async () => {
    try {
      return await getDocspaceAccountToken(pipedriveToken);
    } catch (e) {
      if (isPipedriveUnauthorized(e)) {
        setAppError(AppErrorType.TOKEN_ERROR);
      }

      throw e;
    }
  };

  const getEditorDocspaceConfig = (id: string, frameMode: string) => {
    const config = {
      frameId: DOCSPACE_FRAME_ID,
      src: settings?.url,
      mode: frameMode,
      width: "100%",
      height: "100%",
      id,
      theme: sdk.userSettings.theme === "dark" ? "Dark" : "Base",
      editorGoBack: false,
      locale: getLocaleForDocspace(i18next.language),
      getToken,
      events: {
        onAuthError,
      } as TFrameEvents,
    } as unknown as TFrameConfig;

    return config;
  };

  return (
    <>
      {!docspaceAuthorized && (
        <OnlyofficeDocspaceLogin
          onSuccess={() => setDocspaceAuthorized(true)}
        />
      )}
      {settings?.url && docspaceAuthorized && (
        <div className="w-full h-full flex flex-col items-end">
          <DocSpace
            key={`${fileId}-${mode}`}
            config={getEditorDocspaceConfig(fileId, mode)}
          />
        </div>
      )}
    </>
  );
};
export default EditorPage;
