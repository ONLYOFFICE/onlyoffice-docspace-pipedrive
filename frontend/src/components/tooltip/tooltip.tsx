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
import React, { useState } from "react";

import cx from "classnames";

import Info from "@assets/info.svg";
import InfoHover from "@assets/info-hover.svg";

type TooltipProps = {
  body: JSX.Element | JSX.Element[];
  disabled?: boolean;
};

export const OnlyofficeTooltip: React.FC<TooltipProps> = ({
  body,
  disabled = false,
}) => {
  const [hovered, setHovered] = useState(false);

  const containerStyle = cx({
    "group relative flex flex-col max-w-max items-center justify-center cursor-pointer z-10":
      !disabled,
    "cursor-not-allowed": disabled,
  });

  return (
    <div
      className={containerStyle}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {hovered && !disabled ? <InfoHover /> : <Info />}
      {hovered && !disabled && (
        <div
          className={`absolute top-3/4 w-max max-w-[400px] transform invisible
              border border-pipedrive-color-light-divider-strong dark:border-pipedrive-color-dark-divider-strong rounded shadow-md bg-white dark:bg-pipedrive-color-dark-neutral-100 transition-all
              duration-200 group-hover:translate-y-1 group-hover:visible`}
        >
          <div className="w-full p-4">{body}</div>
        </div>
      )}
    </div>
  );
};
