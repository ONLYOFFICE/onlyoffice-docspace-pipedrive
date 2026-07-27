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

import React, { useContext, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Command, View } from "@pipedrive/app-extensions-sdk";

import { AppContext, AppErrorType } from "@context/AppContext";
import { ButtonColor, OnlyofficeButton } from "@components/button";
import { OnlyofficeSpinner } from "@components/spinner";

import {
  deleteDocspaceAccount,
  getDocspaceAccount,
  getDocspaceOAuthAuthorizeUrl,
  postDocspaceOAuthCallback,
} from "@services/user";

import PipedriveIcon from "@assets/pipedrive-icon-80.svg";
import DocspaceIcon from "@assets/onlyoffice-icon-80.svg";
import SwapArrows from "@assets/icon-swap.svg";
import FolderIcon from "@assets/icon-folder.svg";
import EditFilesIcon from "@assets/icon-edit-files.svg";
import SyncIcon from "@assets/icon-sync.svg";
import ShieldOutline from "@assets/icon-shield-outline.svg";
import LockSmall from "@assets/icon-lock-small.svg";
import Authorized from "@assets/authorized.svg";
import OpenLink from "@assets/open-link.svg";

import { AxiosError } from "axios";
import {
  DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE,
  DocspaceOAuthCallbackMessage,
} from "../../types/docspace";
import { ErrorResponse } from "../../types/error";

export type DocspaceLoginProps = {
  onSuccess?: () => void;
  showUserGuide?: () => void;
};

export const OnlyofficeDocspaceLogin: React.FC<DocspaceLoginProps> = ({
  onSuccess,
  showUserGuide,
}) => {
  const { t } = useTranslation();
  const { sdk, pipedriveToken, settings, setAppError } = useContext(AppContext);

  const [checkingAccount, setCheckingAccount] = useState(true);
  const [docspaceAccountEmail, setDocspaceAccountEmail] = useState<
    string | null
  >(null);
  const [connecting, setConnecting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const oauthPopupRef = useRef<Window | null>(null);

  const fetchDocspaceAccount = async () => {
    try {
      const email = await getDocspaceAccount(pipedriveToken);
      setDocspaceAccountEmail(email);
      return email;
    } catch (e) {
      const data = (e as AxiosError)?.response?.data as ErrorResponse;
      const isDocspaceOAuthError =
        data?.cause === "DocspaceOAuth2AuthorizationException";

      if (
        (e as AxiosError)?.response?.status === 401 &&
        !isDocspaceOAuthError
      ) {
        setAppError(AppErrorType.TOKEN_ERROR);
      }
      setDocspaceAccountEmail(null);
      return null;
    }
  };

  useEffect(() => {
    fetchDocspaceAccount()
      .then((email) => {
        if (email) {
          onSuccess?.();
        }
      })
      .finally(() => setCheckingAccount(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleConnectOAuth = async () => {
    setConnecting(true);

    let authorizeUrl: string;
    try {
      authorizeUrl = await getDocspaceOAuthAuthorizeUrl(pipedriveToken);
    } catch (e) {
      setConnecting(false);
      if (e instanceof AxiosError && e?.response?.status === 401) {
        setAppError(AppErrorType.TOKEN_ERROR);
      } else {
        setAppError(AppErrorType.COMMON_ERROR);
      }
      return;
    }

    oauthPopupRef.current = window.open(
      authorizeUrl,
      "docspace-oauth",
      "width=600,height=720",
    );

    if (!oauthPopupRef.current) {
      setConnecting(false);
    }
  };

  useEffect(() => {
    if (!connecting) {
      return undefined;
    }

    const handleMessage = (
      event: MessageEvent<DocspaceOAuthCallbackMessage>,
    ) => {
      if (event.origin !== window.location.origin) {
        return;
      }
      if (event.data?.type !== DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE) {
        return;
      }

      const { code, state, error } = event.data;
      oauthPopupRef.current = null;

      if (error === "access_denied") {
        setConnecting(false);
        return;
      }

      if (error || !code || !state) {
        setConnecting(false);
        sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "docspace.login.oauth.error",
            "Could not connect to ONLYOFFICE DocSpace. Please try again.",
          ),
        });
        return;
      }

      postDocspaceOAuthCallback(pipedriveToken, code, state)
        .then(async () => {
          await fetchDocspaceAccount();
          onSuccess?.();
        })
        .catch(async () => {
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "docspace.login.oauth.error",
              "Could not connect to ONLYOFFICE DocSpace. Please try again.",
            ),
          });
        })
        .finally(() => setConnecting(false));
    };

    const popupClosedCheck = setInterval(() => {
      if (oauthPopupRef.current?.closed) {
        clearInterval(popupClosedCheck);
        setConnecting(false);
      }
    }, 500);

    window.addEventListener("message", handleMessage);

    return () => {
      window.removeEventListener("message", handleMessage);
      clearInterval(popupClosedCheck);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connecting, pipedriveToken, sdk, onSuccess, t]);

  const handleLogout = async () => {
    const { confirmed } = await sdk.execute(Command.SHOW_CONFIRMATION, {
      title: t("label.warning", "Warning"),
      description:
        t(
          "settings.authorization.deleting.confirm-message",
          "Are you sure you want to log out?",
        ) || "",
      okText: t("button.logout", "Log out"),
    });

    if (!confirmed) {
      return;
    }

    setDeleting(true);
    deleteDocspaceAccount(pipedriveToken)
      .then(async () => {
        setDocspaceAccountEmail(null);
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "settings.authorization.deleting.ok",
            "ONLYOFFICE DocSpace authorization has been successfully deleted",
          ),
        });
      })
      .catch(async () => {
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "error.common",
            "Something went wrong. Please reload the app.",
          ),
        });
      })
      .finally(() => setDeleting(false));
  };

  const handleGoToDeals = async () => {
    await sdk.execute(Command.REDIRECT_TO, { view: View.DEALS });
  };

  const features = [
    {
      key: "rooms",
      Icon: <FolderIcon />,
      text: t(
        "settings.authorization.welcome.option1",
        "Create rooms to work on deal documents",
      ),
    },
    {
      key: "edit",
      Icon: <EditFilesIcon />,
      text: t(
        "settings.authorization.welcome.option2",
        "Edit and collaborate on docs, sheets, slides, forms, PDFs",
      ),
    },
    {
      key: "sync",
      Icon: <SyncIcon />,
      text: t(
        "settings.authorization.welcome.option3",
        "Store your deal data securely in one single place",
      ),
    },
  ];

  const header = (
    <div className="flex items-center gap-3 mb-6">
      <PipedriveIcon />
      <SwapArrows />
      <DocspaceIcon />
    </div>
  );

  const featuresList = (
    <div className="flex flex-col gap-3 self-stretch mb-6">
      {features.map((feature) => (
        <div key={feature.key} className="flex items-center gap-3">
          {feature.Icon}
          <span className="text-sm">{feature.text}</span>
        </div>
      ))}
    </div>
  );

  const infoBox = (Icon: JSX.Element, text: string, detail: string) => (
    <div className="flex items-center gap-3 self-stretch border border-solid border-pipedrive-color-light-divider-strong dark:border-pipedrive-color-dark-divider-strong rounded p-3 mb-6">
      <div>{Icon}</div>
      <div className="flex flex-col text-xs">
        <span className="text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
          {text}
        </span>
        <span className="font-semibold break-all">{detail}</span>
      </div>
    </div>
  );

  if (checkingAccount) {
    return (
      <div className="w-full h-full flex justify-center items-center">
        <OnlyofficeSpinner />
      </div>
    );
  }

  if (docspaceAccountEmail) {
    return (
      <div className="w-full h-full flex justify-center items-center overflow-y-auto">
        <div className="max-w-[380px] w-full flex flex-col items-center py-6 px-4">
          {header}

          <div className="flex flex-col items-center gap-2 text-center mb-6">
            <span className="text-xl font-bold text-pipedrive-color-light-neutral-1000 dark:text-pipedrive-color-dark-neutral-1000">
              {t(
                "settings.authorization.welcome.title",
                "Welcome to DocSpace!",
              )}
            </span>
            <span className="text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
              {t(
                "docspace.login.description",
                "Access your documents without leaving Pipedrive.",
              )}
            </span>
          </div>

          {infoBox(
            <Authorized />,
            t(
              "settings.authorization.status.authorized",
              "You have successfully logged in to your ONLYOFFICE DocSpace account",
            ),
            docspaceAccountEmail,
          )}

          {featuresList}

          <div className="self-stretch mb-3">
            <OnlyofficeButton
              text={t("button.deals", "Go to Deals")}
              color={ButtonColor.PRIMARY}
              fullWidth
              onClick={handleGoToDeals}
            />
          </div>

          <div className="self-stretch mb-3">
            <OnlyofficeButton
              text={t("button.logout", "Log out")}
              color={ButtonColor.NEGATIVE}
              fullWidth
              loading={deleting}
              onClick={handleLogout}
            />
          </div>

          {showUserGuide && (
            <button
              type="button"
              className="flex items-center justify-center text-sm font-semibold text-pipedrive-color-light-blue-600 dark:text-pipedrive-color-dark-blue-600 cursor-pointer hover:underline"
              onClick={showUserGuide}
            >
              {t("settings.authorization.welcome.open-guide", "Open Guide")}
              <OpenLink className="inline-block ml-2" />
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="w-full h-full flex justify-center items-center overflow-y-auto">
      <div className="max-w-[380px] w-full flex flex-col items-center py-6 px-4">
        {header}

        <div className="flex flex-col items-center gap-2 text-center mb-6">
          <span className="text-xl font-bold text-pipedrive-color-light-neutral-1000 dark:text-pipedrive-color-dark-neutral-1000">
            {t("docspace.login.title", "Connect ONLYOFFICE DocSpace")}
          </span>
          <span className="text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
            {t(
              "docspace.login.description",
              "Access your documents without leaving Pipedrive.",
            )}
          </span>
        </div>

        {featuresList}

        {settings?.url &&
          infoBox(
            <ShieldOutline width={32} height={32} />,
            t(
              "docspace.login.portal",
              "You'll connect to your DocSpace portal:",
            ),
            settings.url,
          )}

        <div className="self-stretch mb-3">
          <OnlyofficeButton
            text={t("docspace.login.button.connect", "Sign in with DocSpace")}
            color={ButtonColor.PRIMARY}
            fullWidth
            loading={connecting}
            onClick={handleConnectOAuth}
          />
        </div>

        <div className="flex items-center gap-2 text-xs text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
          <LockSmall />
          <span>
            {t(
              "docspace.login.redirect-hint",
              "You'll be redirected to ONLYOFFICE to sign in securely.",
            )}
          </span>
        </div>
      </div>
    </div>
  );
};
