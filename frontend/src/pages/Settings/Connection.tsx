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

import React, { useState, useContext } from "react";
import { useTranslation } from "react-i18next";
import { Command } from "@pipedrive/app-extensions-sdk";

import { ButtonColor, OnlyofficeButton } from "@components/button";
import { OnlyofficeInput } from "@components/input";
import { OnlyofficeTitle } from "@components/title";

import Connected from "@assets/authorized.svg";
import OpenLink from "@assets/open-link.svg";

import { deleteSettings, putSettings } from "@services/settings";

import { stripTrailingSlash } from "@utils/url";

import { AppContext } from "@context/AppContext";
import { SettingsResponse } from "src/types/settings";
import { AxiosError } from "axios";
import { ErrorResponse } from "src/types/error";
import { getSettings } from "@services/docspace";

export type ConnectionSettingsProps = {
  onChangeSection(): void;
};

export const ConnectionSettings: React.FC<ConnectionSettingsProps> = ({
  onChangeSection,
}) => {
  const { t } = useTranslation();
  const { settings, setSettings, sdk, pipedriveToken, reloadAppContext } =
    useContext(AppContext);

  const [url, setUrl] = useState<string>(settings?.url || "https://");
  const [isInvalidUrl, setIsInvalidUrl] = useState(false);
  const [errorTextInvalidUrl, setErrorTextInvalidUrl] = useState("");
  const [isDisabledUrlInput, setIsDisabledUrlInput] = useState(!!settings?.url);
  const [checkingUrl, setCheckingUrl] = useState(false);

  const [apiKey, setApiKey] = useState<string>(settings?.apiKey || "");
  const [isInvalidApiKey, setIsInvalidApiKey] = useState(false);
  const [errorTextInvalidApiKey, setErrorTextInvalidApiKey] = useState("");
  const [isDisabledApiKeyInput, setIsDisabledApiKeyInput] = useState(true);

  const [isDisabledConnectButton, setIsDisabledConnectButton] = useState(true);
  const [isDisabledSaveButton, setIsDisabledSaveButton] = useState(true);

  const [changing, setChanging] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [disconnecting, setDisconnecting] = useState(false);

  const getSettingsValidationsMessage = (code: string): string => {
    const key = `settings.connection.saving.error.${code}`;
    return t(key, {
      defaultValue: t(
        "settings.connection.saving.error.undefined",
        "Could not save ONLYOFFICE DocSpace settings",
      ),
    });
  };

  const checkDocspaceUrl = (docspaceUrl: string) => {
    if (!url) {
      setIsInvalidUrl(true);
      setErrorTextInvalidUrl(
        t("error.emppty-field", "Please fill out this field"),
      );
      return;
    }

    setIsInvalidUrl(false);
    setIsDisabledUrlInput(true);
    setCheckingUrl(true);

    getSettings(stripTrailingSlash(docspaceUrl))
      .then(() => {
        setIsDisabledApiKeyInput(false);
        setIsDisabledConnectButton(false);
        setIsDisabledSaveButton(false);
      })
      .catch(async () => {
        setIsInvalidUrl(true);
        setIsDisabledUrlInput(false);
        setErrorTextInvalidUrl(
          t(
            "error.incorrect-docspace-address",
            "Incorrect DocSpace address, please try again",
          ),
        );

        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "error.incorrect-docspace-address",
            "Incorrect DocSpace address, please try again",
          ),
        });
      })
      .finally(() => {
        setCheckingUrl(false);
      });
  };

  const handleConnect = async (event: React.SyntheticEvent) => {
    event.preventDefault();

    if (!apiKey) {
      setIsInvalidApiKey(true);
      setErrorTextInvalidApiKey(
        t("error.emppty-field", "Please fill out this field"),
      );
      return;
    }

    setIsInvalidApiKey(false);

    if (url && apiKey) {
      setIsDisabledApiKeyInput(true);
      setIsDisabledConnectButton(true);
      setIsDisabledSaveButton(true);
      setConnecting(true);

      putSettings(pipedriveToken, stripTrailingSlash(url), apiKey)
        .then(async (response: SettingsResponse) => {
          setSettings(response);
          setUrl(response.url);
          setApiKey(response.apiKey);
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.connection.saving.ok",
              "ONLYOFFICE DocSpace connection settings have been successfully saved. You can now proceed to Authorization.",
            ),
          });

          if (changing) {
            reloadAppContext();
          } else {
            onChangeSection();
          }
        })
        .catch(async (e: AxiosError) => {
          const data = e?.response?.data as ErrorResponse;
          if (
            e?.response?.status === 400 &&
            data?.cause === "SettingsValidationException"
          ) {
            setIsInvalidApiKey(true);
            setErrorTextInvalidApiKey(
              t(
                "settings.connection.saving.error.common-invalid-api-key",
                "Invalid DocSpace API key, please try again",
              ),
            );

            await sdk.execute(Command.SHOW_SNACKBAR, {
              message: getSettingsValidationsMessage(
                data.params.validationError,
              ),
              link: {
                url: `${stripTrailingSlash(url)}/developer-tools/api-keys`,
                label: t("button.create-api-key", "Create a key"),
              },
            });
          } else {
            await sdk.execute(Command.SHOW_SNACKBAR, {
              message: t(
                "settings.connection.saving.error.undefined",
                "Could not save ONLYOFFICE DocSpace settings",
              ),
            });
          }
        })
        .finally(() => {
          setIsDisabledApiKeyInput(false);
          setIsDisabledConnectButton(false);
          setConnecting(false);
        });
    }
  };

  const onChange = () => {
    setChanging(true);

    setIsDisabledUrlInput(false);
    setApiKey("");

    setIsDisabledSaveButton(true);
  };

  const onCancelChange = () => {
    setChanging(false);

    setUrl(settings?.url || "");
    setIsInvalidUrl(false);
    setIsDisabledUrlInput(true);

    setApiKey(settings?.apiKey || "");
    setIsInvalidApiKey(false);
    setIsDisabledApiKeyInput(true);
  };

  const handleDisconnect = async () => {
    const { confirmed } = await sdk.execute(Command.SHOW_CONFIRMATION, {
      title: t("label.warning", "Warning"),
      description:
        t(
          "settings.connection.disconnection.description",
          "If you press the Disconnect button, you will not have access to ONLYOFFICE DocSpace. This will remove the connections between Rooms and Deals, and disconnect all users.",
        ) || "",
      okText: t("button.disconnect", "Disconnect"),
    });

    if (confirmed) {
      setDisconnecting(true);
      deleteSettings(pipedriveToken)
        .then(async () => {
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.connection.disconnection.ok",
              "ONLYOFFICE DocSpace successfully disconnected",
            ),
          });
          setSettings(undefined);

          setUrl("");
          setIsDisabledUrlInput(false);

          setApiKey("");
        })
        .catch(async () => {
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.connection.disconnection.error",
              "Could not disconnect ONLYOFFICE DocSpace",
            ),
          });
        })
        .finally(() => {
          setDisconnecting(false);
        });
    }
  };

  const getApiKeyTooltip = () => (
    <div className="flex flex-col items-start">
      <p>
        {t(
          "settings.connection.inputs.api-key.tooltip",
          "Before connecting the app, please go to your DocSpace, create a new API key and enter it here. You can choose Permissions (All) or set restricted access with these required scopes for proper app functionality: Profile (Read), Contacts (Write), and Rooms (Write).",
        )}
      </p>
      <button
        type="button"
        className="flex items-center justify-center pt-2 text-sm font-semibold text-pipedrive-color-light-blue-600 dark:text-pipedrive-color-dark-blue-600 cursor-pointer hover:underline"
        onClick={() => {
          window.open(
            `${stripTrailingSlash(url)}/developer-tools/api-keys`,
            "_blank",
          );
        }}
      >
        {t("button.create-api-key", "Create a key")}
        <OpenLink className="inline-block ml-2" />
      </button>
    </div>
  );

  return (
    <>
      <div className="flex flex-col items-start pl-5 pr-5 pt-5 pb-3">
        <div className="pb-2">
          <OnlyofficeTitle
            text={t(
              "settings.connection.title",
              "Configure connection settings of the ONLYOFFICE DocSpace app",
            )}
          />
        </div>
        {!settings?.url && (
          <div className="pt-3 pb-2">
            {t(
              "settings.connection.subtitle.connect",
              "Connect your ONLYOFFICE DocSpace to Pipedrive",
            )}
          </div>
        )}
        {!!settings?.url && (
          <>
            <div className="flex gap-3 pt-4 pb-3">
              <div>
                <Connected />
              </div>
              <div className="flex justify-center items-center">
                {t(
                  "settings.connection.status.connected",
                  "Your DocSpace has been successfully connected",
                )}
              </div>
            </div>
            <div className="pt-3 pb-2">
              {t(
                "settings.connection.subtitle.change",
                "Change address or disconnect your ONLYOFFICE DocSpace from Pipedrive",
              )}
            </div>
          </>
        )}
      </div>
      <div className="max-w-[390px]">
        <form onSubmit={handleConnect}>
          <div className="pl-5 pr-5 pb-5">
            <OnlyofficeInput
              text={t(
                "settings.connection.inputs.url.title",
                "ONLYOFFICE DocSpace address",
              )}
              description={t(
                "settings.connection.inputs.url.description",
                "Enter the URL of your DocSpace in the field above. For example, https://yourcompany.onlyoffice.com",
              )}
              placeholder="https://"
              value={url}
              valid={!isInvalidUrl}
              errorText={errorTextInvalidUrl}
              required
              disabled={isDisabledUrlInput}
              onChange={(e) => {
                let value = e.target.value.trim();

                if (value.length < 8) {
                  value = "https://";
                }

                value = value.replace(/https?:\/\//g, "");

                setUrl(`https://${value}`);
              }}
              loadingConsent={checkingUrl}
              onConsent={() => {
                checkDocspaceUrl(url);
              }}
            />
          </div>
          <div className="pl-5 pr-5">
            <OnlyofficeInput
              text={t(
                "settings.connection.inputs.api-key.title",
                "ONLYOFFICE DocSpace API key",
              )}
              placeholder="***********"
              tooltip={getApiKeyTooltip()}
              value={apiKey}
              valid={!isInvalidApiKey}
              errorText={errorTextInvalidApiKey}
              required
              disabled={isDisabledApiKeyInput}
              link={{
                text: t("button.create-api-key", "Create a key"),
                href: `${stripTrailingSlash(url)}/developer-tools/api-keys`,
              }}
              onChange={(e) => setApiKey(e.target.value.trim())}
            />
          </div>
          <div className="flex justify-start items-center mt-6 ml-5 gap-2">
            {!settings?.url && (
              <OnlyofficeButton
                disabled={isDisabledConnectButton}
                text={t("button.connect", "Connect")}
                type="submit"
                color={ButtonColor.PRIMARY}
                loading={connecting}
                onClick={handleConnect}
              />
            )}
            {!!settings?.url && !changing && (
              <>
                <OnlyofficeButton
                  text={t("button.change", "Change")}
                  disabled={disconnecting}
                  onClick={onChange}
                />
                <OnlyofficeButton
                  text={t("button.disconnect", "Disconnect")}
                  color={ButtonColor.NEGATIVE}
                  loading={disconnecting}
                  onClick={handleDisconnect}
                />
              </>
            )}
            {changing && (
              <>
                <OnlyofficeButton
                  text={t("button.save", "Save")}
                  type="submit"
                  color={ButtonColor.PRIMARY}
                  disabled={connecting || isDisabledSaveButton}
                  loading={connecting}
                  onClick={handleConnect}
                />
                <OnlyofficeButton
                  text={t("button.cancel", "Cancel")}
                  disabled={connecting}
                  onClick={onCancelChange}
                />
              </>
            )}
          </div>
        </form>
      </div>
    </>
  );
};
