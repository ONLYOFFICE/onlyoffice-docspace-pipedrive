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

type CheckboxProps = {
  id: string;
  text: string;
  checked?: boolean;
  disabled?: boolean;
  onChange?: React.ChangeEventHandler<HTMLInputElement>;
};

export const OnlyofficeCheckbox: React.FC<CheckboxProps> = ({
  id,
  text,
  checked,
  disabled = false,
  onChange,
}) => {
  const cursor = cx({
    "cursor-pointer": !disabled,
    "cursor-not-allowed": disabled,
  });

  return (
    <div className="flex mb-4">
      <input
        id={id}
        type="checkbox"
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        className={`w-5 h-5 text-blue-600 border-2 rounded ${cursor}`}
      />
      <label htmlFor={id} className={`ms-2 ${cursor}`}>
        {text}
      </label>
    </div>
  );
};
