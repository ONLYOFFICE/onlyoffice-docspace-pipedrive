import { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { Command } from "@pipedrive/app-extensions-sdk";
import { DocSpace, TFrameConfig } from "@onlyoffice/docspace-react";

import { OnlyofficeButton } from "@components/button";
import { OnlyofficeInput } from "@components/input";
import { OnlyofficeTitle } from "@components/title";
import { OnlyofficeBackgroundError } from "@layouts/ErrorBackground";

import { AppContext } from "@context/AppContext";

import { postUser, deleteDocspaceAccount } from "@services/user";

import Authorized from "@assets/authorized.svg"
import CommonError from "@assets/common-error.svg";
import { OnlyofficeCheckbox } from "@components/checkbox";

const DOCSPACE_SYSTEM_FRAME_ID="docspace-system-frame"

export const AuthorizationSetting: React.FC = () => {
  const [saving, setSaving] = useState(false);
  const [deleting, setdDeleting] = useState(false);
  const [showValidationMessage, setShowValidationMessage] = useState(false);
  const [email, setEmail] = useState<string | undefined>("");
  const [password, setPassword] = useState<string | undefined>("");

  const { t } = useTranslation();
  const { user, appStatus, setUser, sdk } = useContext(AppContext);

  const [system, setSystem] = useState<boolean>(!appStatus?.isActive);

  const handleLogin = async () => {
    if (email && password) {
      setSaving(true);
    } else {
      setShowValidationMessage(true);
    }
  };

  const handleLogout = async () => {
    setdDeleting(true);
    deleteDocspaceAccount(sdk).then(async () => {
      setEmail("");
      setPassword("");
      setShowValidationMessage(false);
      if (user) {
        setUser({...user, docspaceAccount: null, system: false});
      }
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: t(
          "settings.authorization.deleting.ok",
          "ONLYOFFICE DocSpace authorization has been successfully deleted"
        ),
      });
    })
    .catch(async (e) => {
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: t(
          "background.error.subtitle.common",
          "Something went wrong. Please reload the app."
        ),
      });
    })
    .finally(() => setdDeleting(false));
  };

  const onAppReady = async () => {
    if (email && password) {
      const hashSettings = await window.DocSpace.SDK.frames[DOCSPACE_SYSTEM_FRAME_ID].getHashSettings();
      const passwordHash = await window.DocSpace.SDK.frames[DOCSPACE_SYSTEM_FRAME_ID].createHash(password, hashSettings);
      
      const login = await window.DocSpace.SDK.frames[DOCSPACE_SYSTEM_FRAME_ID].login(email, passwordHash)
      
      if (login.status && login.status !== 200) {
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "docspace.error.login",
            "User authentication failed"
          ),
        });
        setSaving(false);
      } else {
        postUser(sdk, email, passwordHash, system).then(async () => {
          if (user) {
            setUser({...user, docspaceAccount: {userName: email, passwordHash: ""}, system: system});
          }

          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.ok",
              "ONLYOFFICE DocSpace authorization has been successfully saved"
            ),
          });
        })
        .catch(async (e)=> {
          if (e.response?.status === 403 && e.response?.data?.provider === "DOCSPACE") {
            await sdk.execute(Command.SHOW_SNACKBAR, {
              message: t(
                "settings.connection.saving.error.forbidden",
                "The specified user is not a ONLYOFFICE DocSpace administrator"
              ),
            });
            return;
          }

          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.error",
              "Could not save ONLYOFFICE DocSpace authorization"
            ),
          });
        })
        .finally(() => setSaving(false));
      }
    }
  };

  const onAppError = async () => {
    await sdk.execute(Command.SHOW_SNACKBAR, {
      message: t(
        "docspace.error.loading",
        "Error loading ONLYOFFICE DocSpace"
      ),
    });
    delete window.DocSpace;
    setSaving(false);
  };

  const onLoadComponentError = async () => {
    await sdk.execute(Command.SHOW_SNACKBAR, {
      message: t(
        "docspace.error.unreached",
        "ONLYOFFICE DocSpace cannot be reached"
      ),
    });
    setSaving(false);
  };

  return (
    <>
      {(!user?.docspaceSettings || !user?.docspaceSettings.url) && (
        <OnlyofficeBackgroundError
          Icon={<CommonError />}
          title={t("background.error.title", "Error")}
          subtitle={
              `${t("background.error.subtitle.docspace-connection", "You are not connected to ONLYOFFICE DocSpace portal.")} 
                ${(user?.is_admin && user.access.find((a) => a.app === "global" && a.admin))
                  ? t("background.error.hint.admin.docspace-connection", "Please, go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.")
                  : t("background.error.hint.docspace-connection", "Please contact the administrator.")
                }`
          }
        />
      )}
      {user?.docspaceSettings && user?.docspaceSettings.url && (
        <>
          <div className="flex flex-col items-start pl-5 pr-5 pt-5 pb-3">
            <div className="pb-2">
              <OnlyofficeTitle
                text={t("settings.authorization.title", "Login to ONLYOFFICE DocSpace account")}
              />
            </div>
          </div>
          {user?.docspaceAccount && (
            <>
              <div 
                className="inline-flex pl-5 pr-5"
              >
                <Authorized />
                <span
                  className="pl-3"
                >
                  {t("settings.authorization.status.authorized", "You have successfully logged in to your ONLYOFFICE DocSpace account")}
                  {user.system && (
                    <>
                      <br/>
                      {t("settings.authorization.status.system", "Current user is System Admin")}
                    </>
                  )}
                </span>
              </div>
              <div className="flex justify-start items-center mt-4 ml-5">
                <OnlyofficeButton
                  text={t("button.logout", "Logout")}
                  primary
                  disabled={deleting}
                  onClick={handleLogout}
                />
              </div>
            </>
          )}
          {!user?.docspaceAccount && (
            <div className="max-w-[320px]">
              <div className="pl-5 pr-5 pb-2">
                <OnlyofficeInput
                  text={t("settings.authorization.inputs.email", "Email")}
                  valid={showValidationMessage ? !!email : true}
                  value={email}
                  disabled={saving}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div className="pl-5 pr-5 pb-2">
                <OnlyofficeInput
                  text={t("settings.authorization.inputs.password", "Password")}
                  valid={showValidationMessage ? !!password : true}
                  value={password}
                  type="password"
                  disabled={saving}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              {user?.is_admin && user.access.find((a) => a.app === "global" && a.admin) && (
                <div className="pl-5 pr-5">
                  <OnlyofficeCheckbox
                    checked={system}
                    text={t("settings.authorization.inputs.system", "System User")}
                    disabled={!appStatus?.isActive}
                    onChange={(e) => setSystem(!system)}
                  />
                </div>
              )}
              <div className="flex justify-start items-center mt-4 ml-5">
                <OnlyofficeButton
                  text={t("button.login", "Login")}
                  primary
                  disabled={saving}
                  onClick={handleLogin}
                />
              </div>
            </div>
          )}
        </>
      )}
      {saving && user?.docspaceSettings.url && (
        <div hidden>
        <DocSpace
          url={user?.docspaceSettings.url}
          config={
            {
              frameId: DOCSPACE_SYSTEM_FRAME_ID,
              mode: "system",
              events: {
                onAppReady: onAppReady,
                onAppError: onAppError
              } as unknown
            } as TFrameConfig
          }
          onLoadComponentError={onLoadComponentError}
        />
      </div>
      )}
    </>
  );
};
