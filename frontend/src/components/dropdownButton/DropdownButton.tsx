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

import React, { useEffect, useRef, useState } from "react";
import cx from "classnames";

import Plus from "@assets/plus.svg";
import TriangleDown from "@assets/triangle-down.svg";

export enum DropdownButtonColor {
  PRIMARY,
  SECONDARY,
  NEGATIVE,
}

export type DropdownButtonOptions = {
  id: string;
  label: string;
};

export type DropdownButtonProps = {
  text: string;
  disabled?: boolean;
  loading?: boolean;
  color?: DropdownButtonColor;
  fullWidth?: boolean;
  options: Array<DropdownButtonOptions>;
  onClick(optionId: string): void;
};

export const OnlyofficeDropdownButton: React.FC<DropdownButtonProps> = ({
  text,
  loading = false,
  disabled = loading || false,
  color = DropdownButtonColor.SECONDARY,
  fullWidth = false,
  options,
  onClick,
}) => {
  const classes = cx({
    "flex justify-center items-center p-1": true,
    "text-base font-bold text-ellipsis truncate": true,
    "h-[32px]": true,
    "w-full": fullWidth,
    "cursor-pointer": !disabled,
    "cursor-wait": loading,
    "bg-opacity-50 cursor-not-allowed": disabled,

    "text-white bg-pipedrive-color-light-green-600":
      color === DropdownButtonColor.PRIMARY,
    "hover:bg-pipedrive-color-light-green-700":
      color === DropdownButtonColor.PRIMARY && !disabled,
    "dark:bg-pipedrive-color-dark-green-600":
      color === DropdownButtonColor.PRIMARY,
    "dark:hover:bg-pipedrive-color-dark-green-700":
      color === DropdownButtonColor.PRIMARY && !disabled,

    "text-white bg-pipedrive-color-light-red-600":
      color === DropdownButtonColor.NEGATIVE,
    "hover:bg-pipedrive-color-light-red-700":
      color === DropdownButtonColor.NEGATIVE && !disabled,
    "dark:bg-pipedrive-color-dark-red-600":
      color === DropdownButtonColor.NEGATIVE,
    "dark:hover:bg-pipedrive-color-dark-red-700":
      color === DropdownButtonColor.NEGATIVE && !disabled,

    "border border-solid bg-white hover:bg-pipedrive-color-light-neutral-100":
      color === DropdownButtonColor.SECONDARY,
    "dark:text-pipedrive-color-dark-neutral-1000":
      color === DropdownButtonColor.SECONDARY,
    "dark:border-pipedrive-color-dark-divider-strong":
      color === DropdownButtonColor.SECONDARY,
    "dark:bg-pipedrive-color-dark-neutral-100":
      color === DropdownButtonColor.SECONDARY,
    "dark:hover:bg-pipedrive-color-dark-neutral-200":
      color === DropdownButtonColor.SECONDARY && !disabled,
  });

  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownButtonRef = useRef<HTMLButtonElement>(null);
  const dropdownWrapperRef = useRef<HTMLDivElement>(null);

  function useOutsideClickListener(
    wraperRef: React.RefObject<HTMLDivElement>,
    buttonRef: React.RefObject<HTMLButtonElement>,
  ) {
    useEffect(() => {
      function handleClickOutside(event: MouseEvent | TouchEvent) {
        if (
          wraperRef.current &&
          !wraperRef.current.contains(event.target as Node) &&
          buttonRef.current &&
          !buttonRef.current.contains(event.target as Node)
        ) {
          setShowDropdown(false);
        }
      }
      document.addEventListener("mousedown", handleClickOutside);
      return () => {
        document.removeEventListener("mousedown", handleClickOutside);
      };
    }, [wraperRef, buttonRef]);
  }

  useOutsideClickListener(dropdownWrapperRef, dropdownButtonRef);

  return (
    <div className="flex flex-col gap-1">
      <div className="flex gap-[1px]">
        <button
          type="button"
          disabled={disabled}
          className={`rounded-l ${classes} min-w-[62px]`}
          onClick={() => onClick(options[0].id)}
        >
          {!loading && (
            <>
              <div className="px-1">
                <Plus />
              </div>
              <div className="px-1 pr-3">{text}</div>
            </>
          )}
          {loading && (
            <div className="text-center">
              <div role="status">
                <svg
                  aria-hidden="true"
                  role="status"
                  className="inline w-5 h-5  text-white animate-spin"
                  viewBox="0 0 100 101"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z"
                    fill="#E5E7EB"
                  />
                  <path
                    d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z"
                    fill="currentColor"
                  />
                </svg>
              </div>
            </div>
          )}
        </button>

        <button
          ref={dropdownButtonRef}
          type="button"
          aria-label="show"
          disabled={disabled}
          className={`rounded-r ${classes} min-w-[0]`}
          onClick={() => setShowDropdown(!showDropdown)}
        >
          <div className="px-1">
            <TriangleDown />
          </div>
        </button>
      </div>

      {showDropdown && (
        <div
          id="dropdown"
          ref={dropdownWrapperRef}
          className="bg-white border rounded shadow-md p-2
            dark:bg-pipedrive-color-dark-neutral-200 dark:border-pipedrive-color-dark-divider-strong"
        >
          <ul>
            {options.map((option) => (
              <li key={option.id}>
                <button
                  type="button"
                  className="w-full text-left px-2 py-[6px] rounded
                    text-base text-ellipsis
                    hover:bg-pipedrive-color-light-blue-600 hover:text-white
                    dark:hover:bg-pipedrive-color-dark-blue-600"
                  onClick={() => {
                    setShowDropdown(false);
                    onClick(option.id);
                  }}
                >
                  {option.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
