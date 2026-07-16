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

/* eslint-disable jsx-a11y/label-has-associated-control */
import React from "react";
import cx from "classnames";
import { ButtonColor, OnlyofficeButton } from "@components/button";

import RightArrow from "@assets/right-arrow.svg";
import OpenEye from "@assets/open-eye.svg";
import ClosedEye from "@assets/closed-eye.svg";

import { OnlyofficeTooltip } from "@components/tooltip";

type InputProps = {
  text: string;
  value?: string;
  description?: string;
  placeholder?: string;
  type?: "text" | "password";
  tooltip?: JSX.Element | JSX.Element[];
  errorText?: string;
  valid?: boolean;
  required?: boolean;
  disabled?: boolean;
  autocomplete?: boolean;
  loadingConsent?: boolean;
  link?: {
    text: string;
    href: string;
  };
  onChange?: React.ChangeEventHandler<HTMLInputElement>;
  onConsent?: React.MouseEventHandler<HTMLButtonElement>;
};

export const OnlyofficeInput: React.FC<InputProps> = ({
  text,
  value,
  description,
  placeholder,
  type = "text",
  tooltip,
  errorText = "Please fill out this field",
  valid = true,
  required = false,
  disabled = false,
  autocomplete = false,
  loadingConsent = false,
  link,
  onChange,
  onConsent,
}) => {
  const istyle = cx({
    "appearance-none block select-auto h-8 px-2 py-1": true,
    "dark:bg-pipedrive-color-dark-neutral-100": true,
    "w-full border rounded": true,
    "border-pipedrive-color-light-divider-strong dark:border-pipedrive-color-dark-divider-strong":
      valid,
    "border-red-600": !valid,
    "text-pipedrive-color-light-neutral-500 dark:text-pipedrive-color-dark-neutral-500 cursor-not-allowed":
      disabled,
  });

  const pstyle = cx({
    hidden: valid,
  });

  const astyle = cx({
    "text-sm text-blue-600": true,
    "cursor-not-allowed text-opacity-50": disabled,
    "hover:underline": !disabled,
  });

  const [showPassword, setShowPassword] = React.useState(false);
  const [inputType, setInputType] = React.useState(type);

  return (
    <div>
      <div className="flex gap-2">
        <label>
          {text}
          {required && <span className="text-red-600">*</span>}
        </label>
        {tooltip && <OnlyofficeTooltip body={tooltip} disabled={disabled} />}
      </div>
      <div className="relative mt-1">
        <input
          value={value}
          placeholder={placeholder}
          className={istyle}
          required
          autoCorrect={autocomplete ? undefined : "off"}
          autoComplete={autocomplete ? undefined : "off"}
          type={inputType}
          onChange={onChange}
          disabled={disabled}
        />
        {onConsent && (
          <div className="absolute top-0 right-0">
            <OnlyofficeButton
              text=""
              disabled={disabled}
              embedded={{
                icon: <RightArrow />,
              }}
              loading={loadingConsent}
              onClick={onConsent}
              color={ButtonColor.PRIMARY}
            />
          </div>
        )}
        {type === "password" && (
          <div className="absolute top-0 right-0">
            <button
              className="flex items-center justify-center p-2"
              type="button"
              disabled={disabled}
              onClick={() => {
                setShowPassword(!showPassword);
                setInputType(!showPassword ? "text" : "password");
              }}
            >
              {showPassword ? <ClosedEye /> : <OpenEye />}
            </button>
          </div>
        )}
      </div>
      <p className={`text-red-600 text-xs ${pstyle}`}>{errorText}</p>
      {valid && description && (
        <div className="mt-1 text-xs text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
          {description}
        </div>
      )}
      {link && (
        <div className="text-end">
          <a
            href={disabled ? undefined : link.href}
            target="_blank"
            className={astyle}
            rel="noreferrer noopener"
          >
            {link.text}
          </a>
        </div>
      )}
    </div>
  );
};
