import React, { useContext, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Command, View } from "@pipedrive/app-extensions-sdk";
import { DocSpace } from "@onlyoffice/docspace-react";
import { TFrameConfig } from "@onlyoffice/docspace-sdk-js/dist/types/types";
import { SDKInstance } from "@onlyoffice/docspace-sdk-js/dist/types/instance";

import { ButtonColor, OnlyofficeButton } from "@components/button";
import { OnlyofficeInput } from "@components/input";
import { OnlyofficeTitle } from "@components/title";
import { OnlyofficeBackgroundError } from "@layouts/ErrorBackground";

import { AppContext } from "@context/AppContext";

import {
  putDocspaceAccount,
  deleteDocspaceAccount,
  getDocspaceOAuthAuthorizeUrl,
  postDocspaceOAuthCallback,
} from "@services/user";

import Authorized from "@assets/authorized.svg";
import NotAvailable from "@assets/not-available.svg";
import Welcome from "@assets/welcome.svg";

import { ErrorResponse } from "src/types/error";
import {
  DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE,
  DocspaceOAuthCallbackMessage,
} from "../../types/docspace";

const DOCSPACE_SYSTEM_FRAME_ID = "authorization-docspace-system-frame";

export type AuthorizationSettingProps = {
  showUserGuide(): void;
  onChangeSection(): void;
};

export const AuthorizationSetting: React.FC<AuthorizationSettingProps> = ({
  showUserGuide,
  onChangeSection,
}) => {
  const { t } = useTranslation();
  const {
    user,
    settings,
    setSettings,
    setUser,
    sdk,
    pipedriveToken,
    reloadAppContext,
  } = useContext(AppContext);

  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [connectingOAuth, setConnectingOAuth] = useState(false);
  const oauthPopupRef = useRef<Window | null>(null);

  const [email, setEmail] = useState<string | undefined>("");
  const [isInvalidEmail, setIsInvalidEmail] = useState(false);
  const [errorTextInvalidEmail, setErrorTextInvalidEmail] = useState("");

  const [password, setPassword] = useState<string | undefined>("");
  const [isInvalidPassword, setIsInvalidPassword] = useState(false);
  const [errorTextInvalidPassword, setErrorTextInvalidPassword] = useState("");

  let docspaceInstance: SDKInstance;

  const handleLogin = async (event: React.SyntheticEvent) => {
    event.preventDefault();

    if (!email || !password) {
      if (!email) {
        setIsInvalidEmail(true);
        setErrorTextInvalidEmail(
          t("error.empty-field", "This field is required"),
        );
      } else {
        setIsInvalidEmail(false);
      }

      if (!password) {
        setIsInvalidPassword(true);
        setErrorTextInvalidPassword(
          t("error.empty-field", "This field is required"),
        );
      } else {
        setIsInvalidPassword(false);
      }

      return;
    }

    setIsInvalidEmail(false);
    setIsInvalidPassword(false);

    setSaving(true);
  };

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
        setEmail("");
        setPassword("");
        if (user && settings) {
          setUser({ ...user, docspaceAccount: null });
        }
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

  const handleConnectOAuth = async () => {
    setConnectingOAuth(true);

    let authorizeUrl: string;
    try {
      authorizeUrl = await getDocspaceOAuthAuthorizeUrl(pipedriveToken);
    } catch {
      setConnectingOAuth(false);
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: t(
          "settings.authorization.saving.error",
          "Could not save ONLYOFFICE DocSpace authorization",
        ),
      });
      return;
    }

    oauthPopupRef.current = window.open(
      authorizeUrl,
      "docspace-oauth",
      "width=600,height=720",
    );

    if (!oauthPopupRef.current) {
      setConnectingOAuth(false);
    }
  };

  useEffect(() => {
    if (!connectingOAuth) {
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

      if (error || !code || !state) {
        setConnectingOAuth(false);
        return;
      }

      postDocspaceOAuthCallback(pipedriveToken, code, state)
        .then(async () => {
          reloadAppContext();
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.ok",
              "ONLYOFFICE DocSpace authorization has been successfully saved",
            ),
          });
          showUserGuide();
        })
        .catch(async () => {
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.error",
              "Could not save ONLYOFFICE DocSpace authorization",
            ),
          });
        })
        .finally(() => setConnectingOAuth(false));
    };

    const popupClosedCheck = setInterval(() => {
      if (oauthPopupRef.current?.closed) {
        clearInterval(popupClosedCheck);
        setConnectingOAuth(false);
      }
    }, 500);

    window.addEventListener("message", handleMessage);

    return () => {
      window.removeEventListener("message", handleMessage);
      clearInterval(popupClosedCheck);
    };
  }, [
    connectingOAuth,
    pipedriveToken,
    reloadAppContext,
    sdk,
    showUserGuide,
    t,
  ]);

  const onAppReady = async () => {
    if (email && password && docspaceInstance) {
      const loginTimeout = setTimeout(async () => {
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: `${t("docspace.error.login", "User authentication failed")} (Timeout)`,
        });
        setSaving(false);
      }, 15000);

      const hashSettings = await docspaceInstance.getHashSettings();
      const passwordHash = (await docspaceInstance.createHash(
        password,
        hashSettings,
      )) as unknown as string;

      const login = (await docspaceInstance.login(email, passwordHash)) as {
        status: number;
      };

      clearTimeout(loginTimeout);

      if (login.status && login.status !== 200) {
        setIsInvalidEmail(true);
        setIsInvalidPassword(true);
        setErrorTextInvalidEmail(
          t("docspace.error.login", "User authentication failed"),
        );

        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "settings.authorization.unsuccessful",
            "Invalid authorization credentials. Please check your Email and password and try to log in again.",
          ),
        });
        setSaving(false);
      } else {
        const userInfo = (await docspaceInstance.getUserInfo()) as {
          id: string;
        };

        putDocspaceAccount(pipedriveToken, userInfo.id, email, passwordHash)
          .then(async () => {
            if (user) {
              setUser({
                ...user,
                docspaceAccount: {
                  userName: email,
                  passwordHash: "",
                  token: null,
                },
              });
            }

            await sdk.execute(Command.SHOW_SNACKBAR, {
              message: t(
                "settings.authorization.saving.ok",
                "ONLYOFFICE DocSpace authorization has been successfully saved",
              ),
            });
            showUserGuide();
          })
          .catch(async (e) => {
            const data = e?.response?.data as ErrorResponse;
            if (
              e?.response?.status === 503 &&
              data?.cause === "DocspaceUrlNotFoundException"
            ) {
              if (settings) {
                setSettings({
                  ...settings,
                  url: "",
                });
              }
              return;
            }

            if (
              e?.response?.status === 503 &&
              data?.cause === "DocspaceApiKeyNotFoundException"
            ) {
              if (settings) {
                setSettings({
                  ...settings,
                  apiKey: "",
                  isApiKeyValid: false,
                });
              }
              return;
            }

            if (
              e?.response?.status === 503 &&
              data?.cause === "DocspaceApiKeyInvalidException"
            ) {
              if (settings) {
                setSettings({
                  ...settings,
                  isApiKeyValid: false,
                });
              }
              return;
            }

            await sdk.execute(Command.SHOW_SNACKBAR, {
              message: t(
                "settings.authorization.saving.error",
                "Could not save ONLYOFFICE DocSpace authorization",
              ),
            });
          })
          .finally(() => setSaving(false));
      }
    }
  };

  const onAppError = async () => {
    await sdk.execute(Command.SHOW_SNACKBAR, {
      message: t("docspace.error.loading", "Error loading ONLYOFFICE DocSpace"),
    });

    if (docspaceInstance) {
      docspaceInstance.destroyFrame();
    }

    setSaving(false);
  };

  return (
    <>
      {(!settings?.url || !settings?.apiKey) && (
        <OnlyofficeBackgroundError
          Icon={<NotAvailable />}
          title={t("background.error.title.not-available", "Not yet available")}
          subtitle={
            user?.isAdmin
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
                )}`
          }
          button={
            !user?.isAdmin
              ? {
                  text: t("button.reload", "Reload"),
                  onClick: () => reloadAppContext(),
                }
              : {
                  text: t("button.settings", "Go to Settings"),
                  onClick: onChangeSection,
                }
          }
        />
      )}
      {settings?.apiKey && !settings?.isApiKeyValid && (
        <OnlyofficeBackgroundError
          Icon={<NotAvailable />}
          title={t("background.error.title.not-available", "Not yet available")}
          subtitle={`${t(
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
          }`}
          button={
            !user?.isAdmin
              ? {
                  text: t("button.reload", "Reload"),
                  onClick: () => reloadAppContext(),
                }
              : undefined
          }
        />
      )}
      {settings?.url && settings?.apiKey && settings?.isApiKeyValid && (
        <>
          <div className="flex flex-col items-start pl-5 pr-5 pt-5 pb-3">
            <div className="pb-2">
              <OnlyofficeTitle
                text={t(
                  "settings.authorization.title",
                  "Log into your connected ONLYOFFICE DocSpace to start using it within Pipedrive",
                )}
              />
            </div>
            {!user?.docspaceAccount && (
              <div className="pt-3 pb-2">
                {t(
                  "settings.authorization.subtitle.address",
                  "Your connected DocSpace address is",
                )}{" "}
                <span className="font-semibold text-pipedrive-color-light-green-600 dark:text-pipedrive-color-dark-green-600">
                  {settings.url}
                </span>
              </div>
            )}
          </div>
          {!user?.docspaceAccount && (
            <div className="flex justify-start items-center pb-3 ml-5">
              <OnlyofficeButton
                text={t(
                  "settings.authorization.oauth.connect",
                  "Connect to DocSpace",
                )}
                color={ButtonColor.PRIMARY}
                loading={connectingOAuth}
                onClick={handleConnectOAuth}
              />
            </div>
          )}
          {!user?.docspaceAccount && (
            <div className="max-w-[390px]">
              <form onSubmit={handleLogin}>
                <div className="pl-5 pr-5 pb-[14px]">
                  <OnlyofficeInput
                    text={t("settings.authorization.inputs.email", "Email")}
                    placeholder={t(
                      "settings.authorization.inputs.email",
                      "Email",
                    )}
                    required
                    valid={!isInvalidEmail}
                    errorText={errorTextInvalidEmail}
                    value={email}
                    disabled={saving}
                    onChange={(e) => setEmail(e.target.value.trim())}
                  />
                </div>
                <div className="pl-5 pr-5">
                  <OnlyofficeInput
                    text={t(
                      "settings.authorization.inputs.password",
                      "Password",
                    )}
                    placeholder={t(
                      "settings.authorization.inputs.password",
                      "Password",
                    )}
                    required
                    valid={!isInvalidPassword}
                    errorText={errorTextInvalidPassword}
                    value={password}
                    type="password"
                    disabled={saving}
                    link={{
                      text: t("button.forgot-password", "Forgot password?"),
                      href: `${settings.url}/profile`,
                    }}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </div>
                <div className="flex justify-start items-center mt-6 ml-5">
                  <OnlyofficeButton
                    text={t("button.login", "Log in")}
                    type="submit"
                    color={ButtonColor.PRIMARY}
                    loading={saving}
                    onClick={handleLogin}
                  />
                </div>
              </form>
            </div>
          )}
          {user?.docspaceAccount && (
            <>
              <div className="flex gap-3 mt-1 pb-2 pl-5 pr-5">
                <div>
                  <Authorized />
                </div>
                <div className="flex justify-center items-center">
                  <p>
                    {t(
                      "settings.authorization.status.authorized",
                      "You have successfully logged in to your ONLYOFFICE DocSpace account",
                    )}{" "}
                    <span className="font-semibold text-pipedrive-color-light-green-600 dark:text-pipedrive-color-dark-green-600">
                      {user.docspaceAccount.userName}
                    </span>
                  </p>
                </div>
              </div>
              <div className="flex justify-start items-center mt-4 ml-5">
                <OnlyofficeButton
                  text={t("button.logout", "Log out")}
                  color={ButtonColor.NEGATIVE}
                  loading={deleting}
                  onClick={handleLogout}
                />
              </div>
              <div className="pt-4">
                <OnlyofficeBackgroundError
                  Icon={<Welcome />}
                  title={t(
                    "settings.authorization.welcome.title",
                    "Welcome to DocSpace!",
                  )}
                  options={[
                    t(
                      "settings.authorization.welcome.option1",
                      "Create rooms to work on deal documents",
                    ),
                    t(
                      "settings.authorization.welcome.option2",
                      "Edit and collaborate on docs, sheets, slides, forms, PDFs",
                    ),
                    t(
                      "settings.authorization.welcome.option3",
                      "Store your deal data securely in one single place",
                    ),
                  ]}
                  button={{
                    text: t("button.deals", "Go to Deals"),
                    onClick: async () => {
                      await sdk.execute(Command.REDIRECT_TO, {
                        view: View.DEALS,
                      });
                    },
                  }}
                  link={{
                    text: t(
                      "settings.authorization.welcome.open-guide",
                      "Open Guide",
                    ),
                    onClick: showUserGuide,
                  }}
                />
              </div>
            </>
          )}
        </>
      )}
      {saving && settings?.url && (
        <div hidden>
          <DocSpace
            url={settings.url}
            config={
              {
                frameId: DOCSPACE_SYSTEM_FRAME_ID,
                mode: "system",
                events: {
                  onAppReady,
                  onAppError,
                } as unknown,
              } as TFrameConfig
            }
            onSetDocspaceInstance={(instance) => {
              docspaceInstance = instance;
            }}
          />
        </div>
      )}
    </>
  );
};
