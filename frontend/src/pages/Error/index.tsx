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

import React, { useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  ErrorProps,
  OnlyofficeBackgroundError,
} from "@layouts/ErrorBackground";

import { AppContext, AppErrorType } from "@context/AppContext";

import CommonError from "@assets/common-error.svg";
import NotAvailable from "@assets/not-available.svg";
import DenniedError from "@assets/dennied-error.svg";

import { Command, View } from "@pipedrive/app-extensions-sdk";
import { requestAccessToRoom } from "@services/room";
import { getCurrentURL } from "@utils/url";
import { ErrorResponse } from "src/types/error";

type ErrorPageProps = {
  children?: JSX.Element | JSX.Element[];
};

export const ErrorPage: React.FC<ErrorPageProps> = ({ children }) => {
  const [errorProps, setErrorProps] = useState<ErrorProps | undefined>();
  const [processingRequestAccess, setProcessingRequestAccess] = useState(false);

  const { t } = useTranslation();
  const { sdk, pipedriveToken, user, appError, setAppError, reloadAppContext } =
    useContext(AppContext);

  useEffect(() => {
    const { parameters } = getCurrentURL();

    if (appError !== undefined) {
      sdk.execute(Command.RESIZE, { height: 400 }).catch(() => {});
    }

    switch (appError) {
      case AppErrorType.COMMON_ERROR: {
        setErrorProps({
          Icon: <CommonError />,
          title: t("background.error.title.common", "Something went wrong"),
          subtitle: t(
            "background.error.subtitle.common",
            "Could not fetch plugin settings. Please reload the Pipedrive window.",
          ),
          button: {
            text: t("button.reload", "Reload"),
            onClick: () => reloadAppContext(),
          },
        });
        break;
      }
      case AppErrorType.TOKEN_ERROR: {
        setErrorProps({
          Icon: <CommonError className="mb-5" />,
          title: t("background.error.title.common", "Something went wrong"),
          subtitle: t(
            "background.error.subtitle.token-expired",
            "The document security token has expired. Please re-authorize the app.",
          ),
          button: {
            text: t("button.reauthorize", "Re-authorize") || "Re-authorize",
            onClick: () =>
              window.open(
                `${process.env.BACKEND_URL}/oauth2/authorization/pipedrive`,
                "_blank",
              ),
          },
        });
        break;
      }
      case AppErrorType.PLUGIN_NOT_AVAILABLE: {
        setErrorProps({
          Icon: <NotAvailable />,
          title: t("background.error.title.not-available", "Not yet available"),
          subtitle: user?.isAdmin
            ? `${t(
                "background.error.subtitle.docspace-connection",
                "You are not connected to ONLYOFFICE DocSpace.",
              )} ${t(
                "background.error.hint.admin.docspace-connection",
                "Please go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.",
              )}`
            : `${t(
                "background.error.subtitle.plugin.not-active.message",
                "ONLYOFFICE DocSpace App is not yet available.",
              )} ${t(
                "background.error.subtitle.plugin.not-active.help",
                "Please wait until a Pipedrive Administrator configures the app settings.",
              )}`,
          button: {
            text: user?.isAdmin
              ? t("button.settings", "Go to Settings")
              : t("button.reload", "Reload"),
            onClick: user?.isAdmin
              ? () => sdk.execute(Command.REDIRECT_TO, { view: View.SETTINGS })
              : () => reloadAppContext(),
          },
        });
        break;
      }
      case AppErrorType.DOCSPACE_ROOM_NOT_FOUND: {
        setErrorProps({
          Icon: <DenniedError />,
          title: t(
            "background.error.title.create-room",
            "Sorry, you don't have a permission to create rooms",
          ),
          subtitle: t(
            "background.error.subtitle.create-room",
            "Please ask a Pipedrive Administrator to create a room or contact a DocSpace admin to upgrade your role.",
          ),
        });
        break;
      }
      case AppErrorType.DOCSPACE_ROOM_NO_ACCESS: {
        setErrorProps({
          Icon: <DenniedError />,
          title: t(
            "background.error.title.no-room-access",
            "Sorry, you don't have access to this room",
          ),
          subtitle: t(
            "background.error.subtitle.no-room-access",
            "Please try to request access",
          ),
          button: {
            text:
              t("button.request-access", "Request access") || "Request access",
            loading: processingRequestAccess,
            onClick: () => {
              setProcessingRequestAccess(true);
              requestAccessToRoom(
                pipedriveToken,
                Number(parameters.get("selectedIds")),
              )
                .then(async () => {
                  await sdk.execute(Command.SHOW_SNACKBAR, {
                    message: t(
                      "room.request-access.ok",
                      "You have been successfully granted access to the ONLYOFFICE DocSpace room",
                    ),
                  });
                  setAppError(undefined);
                })
                .catch(async (e) => {
                  const data = e?.response?.data as ErrorResponse;
                  if (
                    e?.response?.status === 503 &&
                    data?.cause === "DocspaceUrlNotFoundException"
                  ) {
                    setAppError(AppErrorType.PLUGIN_NOT_AVAILABLE);
                    return;
                  }

                  if (
                    e?.response?.status === 503 &&
                    data?.cause === "DocspaceApiKeyNotFoundException"
                  ) {
                    setAppError(AppErrorType.PLUGIN_NOT_AVAILABLE);
                    return;
                  }

                  if (
                    e?.response?.status === 503 &&
                    data?.cause === "DocspaceApiKeyInvalidException"
                  ) {
                    setAppError(AppErrorType.DOCSPACE_INVALID_API_KEY);
                    return;
                  }

                  if (
                    e?.response?.status === 403 &&
                    data?.cause === "RequestAccessToRoomException"
                  ) {
                    await sdk.execute(Command.SHOW_SNACKBAR, {
                      message: t(
                        "room.request-access.forbidden",
                        "Error getting access to the ONLYOFFICE DocSpace room, because current user is not deal follower.",
                      ),
                    });
                    return;
                  }

                  await sdk.execute(Command.SHOW_SNACKBAR, {
                    message: t(
                      "room.request-access.error",
                      "Error getting access to the ONLYOFFICE DocSpace room",
                    ),
                  });
                })
                .finally(() => setProcessingRequestAccess(false));
            },
          },
        });
        break;
      }
      case AppErrorType.DOCSPACE_UNREACHABLE: {
        setErrorProps({
          Icon: <NotAvailable />,
          title: t("background.error.title.unreached", "Cannot be reached"),
          subtitle: `${t(
            "docspace.error.unreached",
            "ONLYOFFICE DocSpace cannot be reached.",
          )} 
              ${
                user?.isAdmin
                  ? t(
                      "background.error.hint.admin.docspace-connection",
                      "Please go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.",
                    )
                  : t(
                      "background.error.hint.docspace-connection",
                      "Please contact the administrator.",
                    )
              }`,
          button: user?.isAdmin
            ? {
                text: t("button.settings", "Go to Settings"),
                onClick: () =>
                  sdk.execute(Command.REDIRECT_TO, { view: View.SETTINGS }),
              }
            : undefined,
        });
        break;
      }
      case AppErrorType.DOCSPACE_INVALID_API_KEY: {
        setErrorProps({
          Icon: <NotAvailable />,
          title: t("background.error.title.not-available", "Not yet available"),
          subtitle: `${t(
            "background.error.title.docspace-invalid-api-key",
            "The ONLYOFFICE DocSpace API Key is invalid.",
          )} ${
            user?.isAdmin
              ? t(
                  "background.error.hint.admin.docspace-connection",
                  "Please go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.",
                )
              : t(
                  "background.error.hint.docspace-connection",
                  "Please contact the administrator.",
                )
          }`,
          button: {
            text: user?.isAdmin
              ? t("button.settings", "Go to Settings")
              : t("button.reload", "Reload"),
            onClick: user?.isAdmin
              ? () => sdk.execute(Command.REDIRECT_TO, { view: View.SETTINGS })
              : () => reloadAppContext(),
          },
        });
        break;
      }
      case AppErrorType.WEBHOOKS_IS_NOT_INSTALLED: {
        setErrorProps({
          Icon: <CommonError />,
          title: t(
            "background.error.title.webhook.is-not-installed",
            "Synchronization warning",
          ),
          subtitle: user?.isAdmin
            ? t(
                "background.error.hint.webhook.is-not-installed",
                "The synchronization between Deal followers and Room members is not occurring due to a webhook malfunction.",
              )
            : t(
                "background.error.hint.docspace-connection",
                "Please contact the administrator.",
              ),
          button: user?.isAdmin
            ? {
                text: t("button.reinstall", "Reinstall app"),
                onClick: () =>
                  window.open(
                    `${process.env.BACKEND_URL}/oauth2/authorization/pipedrive`,
                    "_blank",
                  ),
              }
            : undefined,
        });
        break;
      }
      default: {
        setErrorProps(undefined);
        break;
      }
    }
  }, [
    sdk,
    pipedriveToken,
    appError,
    t,
    user,
    setAppError,
    processingRequestAccess,
    reloadAppContext,
  ]);

  return (
    <>
      {appError !== undefined && errorProps && (
        <OnlyofficeBackgroundError
          Icon={errorProps.Icon}
          title={errorProps.title || ""}
          subtitle={errorProps.subtitle || ""}
          button={errorProps.button}
        />
      )}
      {appError === undefined && children}
    </>
  );
};
