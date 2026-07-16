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

import React, { useEffect, useMemo, useState } from "react";
import AppExtensionsSDK from "@pipedrive/app-extensions-sdk";
import i18next from "i18next";

import { AxiosError } from "axios";

import { OnlyofficeSpinner } from "@components/spinner";

import { getUser } from "@services/user";
import { getSettings, validateApiKey } from "@services/settings";

import { UserResponse } from "src/types/user";
import { SettingsResponse } from "src/types/settings";
import { PipedriveToken } from "@context/PipedriveToken";
import { useLocation } from "react-router-dom";

type AppContextProps = {
  children?: JSX.Element | JSX.Element[];
};

export enum AppErrorType {
  COMMON_ERROR,
  TOKEN_ERROR,
  PLUGIN_NOT_AVAILABLE,
  DOCSPACE_ROOM_NOT_FOUND,
  DOCSPACE_UNREACHABLE,
  DOCSPACE_INVALID_API_KEY,
  DOCSPACE_ROOM_NO_ACCESS,
  WEBHOOKS_IS_NOT_INSTALLED,
}

export interface IAppContext {
  sdk: AppExtensionsSDK;
  pipedriveToken: PipedriveToken;
  user: UserResponse | undefined;
  setUser: (value: UserResponse) => void;
  settings: SettingsResponse | undefined;
  setSettings: (value: SettingsResponse | undefined) => void;
  appError: AppErrorType | undefined;
  setAppError: (value: AppErrorType | undefined) => void;
  reloadAppContext: () => void;
}

export const AppContext = React.createContext<IAppContext>({} as IAppContext);

export const AppContextProvider: React.FC<AppContextProps> = ({ children }) => {
  const location = useLocation();
  const [sdk, setSDK] = useState<AppExtensionsSDK>();
  const [pipedriveToken, setPipedriveToken] = useState<PipedriveToken>();
  const [user, setUser] = useState<UserResponse>();
  const [settings, setSettings] = useState<SettingsResponse>();
  const [loading, setLoading] = useState(true);
  const [appError, setAppError] = useState<AppErrorType | undefined>();
  const [reload, setReload] = useState<boolean>(false);

  const appContextProviderValue = useMemo(() => {
    const reloadAppContext = () => {
      setReload(!reload);
      setAppError(undefined);
      setLoading(true);
    };

    return {
      sdk,
      pipedriveToken,
      user,
      setUser,
      settings,
      setSettings,
      appError,
      setAppError,
      reloadAppContext,
    } as IAppContext;
  }, [
    sdk,
    pipedriveToken,
    user,
    setUser,
    settings,
    setSettings,
    appError,
    setAppError,
    reload,
  ]);

  useEffect(() => {
    new AppExtensionsSDK()
      .initialize()
      .then(async (s) => {
        setSDK(s);
        try {
          const pipedriveTokenObject = new PipedriveToken(s);
          const userResponse = await getUser(pipedriveTokenObject);
          let settingsResponse = await getSettings(pipedriveTokenObject);

          await i18next.changeLanguage(
            `${userResponse.language.language_code}-${userResponse.language.country_code}`,
          );

          if (settingsResponse.apiKey && !settingsResponse.isApiKeyValid) {
            try {
              settingsResponse = await validateApiKey(pipedriveTokenObject);
            } catch (e) {
              if (e instanceof AxiosError && e?.response?.status !== 400) {
                throw e;
              }
            }
          }

          if (location.pathname !== "/settings") {
            if (!settingsResponse?.url || !settingsResponse.apiKey) {
              setAppError(AppErrorType.PLUGIN_NOT_AVAILABLE);
            } else if (
              settingsResponse.apiKey &&
              !settingsResponse.isApiKeyValid
            ) {
              setAppError(AppErrorType.DOCSPACE_INVALID_API_KEY);
            } else if (!settingsResponse.isWebhooksInstalled) {
              setAppError(AppErrorType.WEBHOOKS_IS_NOT_INSTALLED);
            }
          }

          setUser(userResponse);
          setSettings(settingsResponse);
          setPipedriveToken(pipedriveTokenObject);
        } catch (e) {
          if (e instanceof AxiosError && e?.response?.status === 401) {
            setAppError(AppErrorType.TOKEN_ERROR);
          } else {
            setAppError(AppErrorType.COMMON_ERROR);
          }
        } finally {
          setLoading(false);
        }
      })
      .catch(
        // eslint-disable-next-line no-console
        (e) => console.error(e),
      );
  }, [reload, location]);

  return (
    <>
      {loading && <OnlyofficeSpinner />}
      {!loading && sdk && (
        <AppContext.Provider value={appContextProviderValue}>
          {children}
        </AppContext.Provider>
      )}
    </>
  );
};
