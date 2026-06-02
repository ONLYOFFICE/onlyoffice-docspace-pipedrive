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
import { filesize } from "filesize";
import Select, { InputActionMeta, SingleValue } from "react-select";

import { PipedriveToken } from "@context/PipedriveToken";
import { getDealFiles, getDeals, searchDeals } from "@services/deal";
import { Deal, DealFile } from "src/types/deal";

const MIN_SEARCH_TERM_LENGTH = 2;

type DealSelectorMode = "deal" | "file";

interface DealOption {
  value: number;
  label: string;
  data: Deal;
}

interface DealSelectorProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect?: (deal: Deal) => void | Promise<void>;
  onFileSelect?: (file: DealFile) => void | Promise<void>;
  pipedriveToken: PipedriveToken;
  mode?: DealSelectorMode;
}

export const DealSelector: React.FC<DealSelectorProps> = ({
  isOpen,
  onClose,
  onSelect,
  onFileSelect,
  pipedriveToken,
  mode = "deal",
}) => {
  const [options, setOptions] = useState<DealOption[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [inputValue, setInputValue] = useState("");
  const [selectedDeal, setSelectedDeal] = useState<DealOption | null>(null);
  const [dealFiles, setDealFiles] = useState<DealFile[]>([]);
  const [dealFilesNextStart, setDealFilesNextStart] = useState<number | null>(
    null,
  );
  const [isLoading, setLoading] = useState(false);
  const [isLoadingMore, setLoadingMore] = useState(false);
  const [isLoadingDealFiles, setLoadingDealFiles] = useState(false);
  const [isLoadingMoreDealFiles, setLoadingMoreDealFiles] = useState(false);
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

  const loadDealFiles = useCallback(
    async (dealId: number, start?: number) => {
      const response = await getDealFiles(pipedriveToken, dealId, {
        start,
      });
      const { pagination } = response.additional_data;

      return {
        files: response.data ?? [],
        nextStart: pagination.more_items_in_collection
          ? (pagination.next_start ?? pagination.start + pagination.limit)
          : null,
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
    setSelectedDeal(null);
    setDealFiles([]);
    setDealFilesNextStart(null);
    setSubmitting(false);
  }, [isOpen, loadFirstPage]);

  const loadSelectedDealFiles = async (deal: DealOption) => {
    const requestId = requestIdRef.current + 1;
    requestIdRef.current = requestId;

    setSelectedDeal(deal);
    setInputValue("");
    setDealFiles([]);
    setDealFilesNextStart(null);
    setLoadingDealFiles(true);

    try {
      const result = await loadDealFiles(deal.value);

      if (requestId === requestIdRef.current) {
        setDealFiles(result.files);
        setDealFilesNextStart(result.nextStart);
      }
    } finally {
      if (requestId === requestIdRef.current) {
        setLoadingDealFiles(false);
      }
    }
  };

  const handleChange = async (option: SingleValue<DealOption>) => {
    if (!option || isSubmitting) {
      return;
    }

    if (mode === "file") {
      await loadSelectedDealFiles(option);
      return;
    }

    if (!onSelect) {
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

  const handleFileSelect = async (file: DealFile) => {
    if (isSubmitting || !onFileSelect) {
      return;
    }

    setSubmitting(true);

    try {
      await onFileSelect(file);
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

  const handleDealFilesScroll = async (
    event: React.UIEvent<HTMLDivElement>,
  ) => {
    const target = event.currentTarget;
    const isNearBottom =
      target.scrollHeight - target.scrollTop - target.clientHeight < 40;

    if (
      !isNearBottom ||
      selectedDeal === null ||
      dealFilesNextStart === null ||
      isLoadingMoreDealFiles ||
      isSubmitting
    ) {
      return;
    }

    setLoadingMoreDealFiles(true);

    try {
      const result = await loadDealFiles(
        selectedDeal.value,
        dealFilesNextStart,
      );

      setDealFiles((currentFiles) => {
        const existingIds = new Set(currentFiles.map((file) => file.id));
        const newFiles = result.files.filter(
          (file) => !existingIds.has(file.id),
        );

        return [...currentFiles, ...newFiles];
      });
      setDealFilesNextStart(result.nextStart);
    } finally {
      setLoadingMoreDealFiles(false);
    }
  };

  const getFileTypeLabel = (file: DealFile) =>
    file.file_type || file.name.split(".").pop() || "file";

  const getLoadingMessage = () => {
    if (isSubmitting) {
      return mode === "file" ? "Importing file..." : "Sending file...";
    }

    return isLoadingMore ? "Loading more deals..." : "Loading deals...";
  };

  const title = mode === "file" ? "Select Pipedrive file" : "Select deal";

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
              {title}
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
              value={mode === "file" ? selectedDeal : undefined}
              inputValue={inputValue}
              isDisabled={isSubmitting}
              isLoading={isLoading || isLoadingMore || isSubmitting}
              onChange={handleChange}
              onInputChange={handleInputChange}
              onMenuScrollToBottom={handleMenuScrollToBottom}
              placeholder={
                mode === "file" ? "Select deal..." : "Search deals..."
              }
              autoFocus
              filterOption={null}
              noOptionsMessage={() => "No open deals found"}
              loadingMessage={getLoadingMessage}
              menuPortalTarget={document.body}
              styles={{
                menuPortal: (base) => ({ ...base, zIndex: 9999 }),
              }}
            />
            {mode === "file" && (
              <div
                onScroll={handleDealFilesScroll}
                className="mt-4 max-h-[358px] overflow-y-auto border border-pipedrive-color-light-neutral-200 dark:border-pipedrive-color-dark-divider-strong"
              >
                {selectedDeal === null && (
                  <div className="px-4 py-8 text-center text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                    Select a deal to see its files
                  </div>
                )}

                {selectedDeal !== null && isLoadingDealFiles && (
                  <div className="px-4 py-8 text-center text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                    Loading files...
                  </div>
                )}

                {selectedDeal !== null &&
                  !isLoadingDealFiles &&
                  dealFiles.length === 0 && (
                    <div className="px-4 py-8 text-center text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                      No files found for this deal
                    </div>
                  )}

                {dealFiles.map((file) => (
                  <div
                    key={file.id}
                    className="flex items-center justify-between gap-4 border-b border-pipedrive-color-light-neutral-200 px-4 py-3 last:border-b-0 dark:border-pipedrive-color-dark-divider-strong"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-pipedrive-color-light-neutral-1000 dark:text-pipedrive-color-dark-neutral-1000">
                        {file.name}
                      </p>
                      <p className="text-xs text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                        {getFileTypeLabel(file)} -{" "}
                        {filesize(file.file_size, {
                          round: 1,
                          standard: "jedec",
                        })}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleFileSelect(file)}
                      disabled={isSubmitting}
                      className="shrink-0 text-sm font-semibold text-pipedrive-color-light-blue-600 hover:text-pipedrive-color-light-blue-700 disabled:opacity-60 dark:text-pipedrive-color-dark-blue-600"
                    >
                      Select
                    </button>
                  </div>
                ))}

                {isLoadingMoreDealFiles && (
                  <div className="px-4 py-3 text-center text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                    Loading more files...
                  </div>
                )}
              </div>
            )}
            {isSubmitting && (
              <p className="mt-3 text-sm text-pipedrive-color-light-neutral-700 dark:text-pipedrive-color-dark-neutral-700">
                {mode === "file"
                  ? "Uploading file to DocSpace..."
                  : "Sending file to Pipedrive..."}
              </p>
            )}
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
};
