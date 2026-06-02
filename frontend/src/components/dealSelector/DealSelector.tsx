/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

import React, { useCallback, useEffect, useRef, useState } from "react";
import { Dialog, DialogPanel, DialogTitle } from "@headlessui/react";
import Select, { InputActionMeta, SingleValue } from "react-select";

import { PipedriveToken } from "@context/PipedriveToken";
import { getDeals, searchDeals } from "@services/deal";
import { Deal } from "src/types/deal";

const MIN_SEARCH_TERM_LENGTH = 2;

interface DealOption {
  value: number;
  label: string;
  data: Deal;
}

interface DealSelectorProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (deal: Deal) => void | Promise<void>;
  pipedriveToken: PipedriveToken;
}

export const DealSelector: React.FC<DealSelectorProps> = ({
  isOpen,
  onClose,
  onSelect,
  pipedriveToken,
}) => {
  const [options, setOptions] = useState<DealOption[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [inputValue, setInputValue] = useState("");
  const [isLoading, setLoading] = useState(false);
  const [isLoadingMore, setLoadingMore] = useState(false);
  const [isSubmitting, setSubmitting] = useState(false);
  const requestIdRef = useRef(0);
  const searchTermRef = useRef("");

  const mapDealsToOptions = (deals: Deal[]) =>
    deals.map((deal) => ({
      value: deal.id,
      label: deal.title,
      data: deal,
    }));

  const loadDealOptions = useCallback(
    async (term: string, cursor?: string | null) => {
      if (term.length >= MIN_SEARCH_TERM_LENGTH) {
        const resp = await searchDeals(pipedriveToken, {
          term,
          cursor: cursor ?? undefined,
        });

        return {
          options: mapDealsToOptions(resp.data.items.map(({ item }) => item)),
          nextCursor: resp.additional_data?.next_cursor ?? null,
        };
      }

      const resp = await getDeals(pipedriveToken, {
        cursor: cursor ?? undefined,
      });

      return {
        options: mapDealsToOptions(resp.data ?? []),
        nextCursor: resp.additional_data?.next_cursor ?? null,
      };
    },
    [pipedriveToken],
  );

  const loadFirstPage = useCallback(
    async (value: string) => {
      const term = value.trim();
      const requestId = requestIdRef.current + 1;
      requestIdRef.current = requestId;
      searchTermRef.current = term;

      setLoading(true);

      try {
        const result = await loadDealOptions(term);

        if (requestId === requestIdRef.current) {
          setOptions(result.options);
          setNextCursor(result.nextCursor);
        }
      } finally {
        if (requestId === requestIdRef.current) {
          setLoading(false);
        }
      }
    },
    [loadDealOptions],
  );

  useEffect(() => {
    if (isOpen) {
      setInputValue("");
      loadFirstPage("");
      return;
    }

    setOptions([]);
    setNextCursor(null);
    setInputValue("");
    setSubmitting(false);
  }, [isOpen, loadFirstPage]);

  const handleChange = async (option: SingleValue<DealOption>) => {
    if (!option || isSubmitting) {
      return;
    }

    setSubmitting(true);

    try {
      await onSelect(option.data);
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  const handleInputChange = (value: string, actionMeta: InputActionMeta) => {
    if (actionMeta.action !== "input-change") {
      return value;
    }

    setInputValue(value);
    loadFirstPage(value);

    return value;
  };

  const handleMenuScrollToBottom = async () => {
    if (!nextCursor || isLoadingMore || isSubmitting) {
      return;
    }

    setLoadingMore(true);

    try {
      const result = await loadDealOptions(searchTermRef.current, nextCursor);

      setOptions((currentOptions) => {
        const existingIds = new Set(
          currentOptions.map((option) => option.value),
        );
        const newOptions = result.options.filter(
          (option) => !existingIds.has(option.value),
        );

        return [...currentOptions, ...newOptions];
      });
      setNextCursor(result.nextCursor);
    } finally {
      setLoadingMore(false);
    }
  };

  const getLoadingMessage = () => {
    if (isSubmitting) {
      return "Sending file...";
    }

    return isLoadingMore ? "Loading more deals..." : "Loading deals...";
  };

  return (
    <Dialog
      open={isOpen}
      onClose={isSubmitting ? () => {} : onClose}
      className="relative z-50"
    >
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" />

      <div className="fixed inset-0 flex items-center justify-center p-4">
        <DialogPanel className="w-full max-w-md rounded-lg shadow-xl bg-white dark:bg-pipedrive-color-dark-neutral-100 overflow-visible">
          <div className="flex items-center justify-between px-5 py-4 border-b border-pipedrive-color-light-divider dark:border-pipedrive-color-dark-divider-strong">
            <DialogTitle className="text-base font-bold text-pipedrive-color-light-neutral-1000 dark:text-pipedrive-color-dark-neutral-1000">
              Select deal
            </DialogTitle>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="text-pipedrive-color-light-neutral-600 dark:text-pipedrive-color-dark-neutral-600 hover:text-pipedrive-color-light-neutral-1000 dark:hover:text-pipedrive-color-dark-neutral-1000 text-xl leading-none"
              aria-label="Close"
            >
              ×
            </button>
          </div>

          <div className="p-4">
            <Select<DealOption>
              options={options}
              inputValue={inputValue}
              isDisabled={isSubmitting}
              isLoading={isLoading || isLoadingMore || isSubmitting}
              onChange={handleChange}
              onInputChange={handleInputChange}
              onMenuScrollToBottom={handleMenuScrollToBottom}
              placeholder="Search deals..."
              autoFocus
              filterOption={null}
              noOptionsMessage={() => "No open deals found"}
              loadingMessage={getLoadingMessage}
              menuPortalTarget={document.body}
              styles={{
                menuPortal: (base) => ({ ...base, zIndex: 9999 }),
              }}
            />
            {isSubmitting && (
              <p className="mt-3 text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                Sending file to Pipedrive...
              </p>
            )}
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
};
