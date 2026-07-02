import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useCatalog } from '../hooks/useCatalog';
import type { CategoryItem, UpsertCategoryRequest } from '../types/catalogTypes';

const EMPTY_FORM: UpsertCategoryRequest = {
  name: '',
  description: '',
  rootName: 'SUBJECT',
  parentId: null,
  status: 'ACTIVE',
};

const ROOT_GROUP_META: Record<string, { label: string; description: string }> = {
  SUBJECT: {
    label: 'Nhóm môn học',
    description: 'Danh mục môn học dùng cho hồ sơ gia sư, lớp học, gợi ý phù hợp và hiển thị trang chủ.',
  },
  EDUCATION_LEVEL: {
    label: 'Nhóm cấp học',
    description: 'Danh mục cấp học và nhóm người học để chuẩn hóa hồ sơ và nhu cầu học tập.',
  },
  LOCATION: {
    label: 'Nhóm khu vực',
    description: 'Danh mục khu vực hoạt động để phục vụ tìm kiếm và gợi ý theo địa bàn.',
  },
  SYSTEM_CONFIG: {
    label: 'Nhóm cấu hình hệ thống',
    description: 'Danh mục lựa chọn hệ thống tái sử dụng như hình thức học và các cấu hình nghiệp vụ.',
  },
};

const ROOT_GROUPS = ['SUBJECT', 'EDUCATION_LEVEL', 'LOCATION', 'SYSTEM_CONFIG'] as const;
type RootGroup = (typeof ROOT_GROUPS)[number];

export function CatalogPanel() {
  const [activeRoot, setActiveRoot] = useState<RootGroup>('SUBJECT');
  const { status, categories, error, reload, createCategory, updateCategory, deleteCategory } =
    useCatalog(activeRoot);
  const [form, setForm] = useState<UpsertCategoryRequest>(EMPTY_FORM);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const flatCategories = useMemo(() => flattenCategories(categories), [categories]);
  const activeGroup = useMemo(() => {
    const root = categories[0] ?? null;
    if (!root) {
      return null;
    }

    return {
      root,
      meta: ROOT_GROUP_META[root.name] ?? {
        label: root.name,
        description: root.description ?? 'Nhóm này chưa có mô tả riêng.',
      },
    };
  }, [categories]);
  const selectedCategory =
    selectedCategoryId == null
      ? null
      : flatCategories.find((category) => category.categoryId === selectedCategoryId) ?? null;

  useEffect(() => {
    if (!selectedCategoryId && activeGroup) {
      setForm((current) => ({
        ...current,
        rootName: activeRoot,
        parentId: current.parentId ?? activeGroup.root.categoryId,
      }));
    }
  }, [activeGroup, activeRoot, selectedCategoryId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);
    setIsSubmitting(true);

    try {
      if (selectedCategory) {
        await updateCategory(selectedCategory.categoryId, form);
      } else {
        await createCategory(form);
      }
      resetForm();
    } catch (submitIssue) {
      setSubmitError(extractUiError(submitIssue));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!selectedCategory) {
      return;
    }

    setSubmitError(null);
    setIsSubmitting(true);

    try {
      await deleteCategory(selectedCategory.categoryId);
      resetForm();
    } catch (deleteIssue) {
      setSubmitError(extractUiError(deleteIssue));
    } finally {
      setIsSubmitting(false);
    }
  }

  function selectCategory(category: CategoryItem) {
    setSelectedCategoryId(category.categoryId);
    setSubmitError(null);
    setForm({
      name: category.name,
      description: category.description ?? '',
      rootName: activeRoot,
      parentId: category.parent?.categoryId ?? null,
      status: category.status,
    });
  }

  function resetForm() {
    setSelectedCategoryId(null);
    setSubmitError(null);
    setForm({
      ...EMPTY_FORM,
      rootName: activeRoot,
      parentId: activeGroup?.root.categoryId ?? null,
    });
  }

  function handleSelectRoot(root: RootGroup) {
    setActiveRoot(root);
    setSelectedCategoryId(null);
    setSubmitError(null);
    setForm({
      ...EMPTY_FORM,
      rootName: root,
    });
  }

  function prepareCreateUnder(parent: CategoryItem) {
    setSelectedCategoryId(null);
    setSubmitError(null);
    setForm({
      ...EMPTY_FORM,
      rootName: activeRoot,
      parentId: parent.categoryId,
    });
  }

  return (
    <section className="catalog-page">
      <div className="catalog-shell">
        <div className="catalog-hero">
          <span className="catalog-eyebrow">Danh mục / Quản lý phân loại</span>
          <h1 className="catalog-title">Quản lý danh mục dùng chung cho môn học và lớp học</h1>
          <p className="catalog-subtitle">
            Mỗi nhóm danh mục được quản lý riêng để giữ cấu trúc rõ ràng cho môn học, khu vực, cấp học
            và các cấu hình dùng chung của hệ thống.
          </p>
        </div>

        <section className="catalog-root-switcher">
          {ROOT_GROUPS.map((root) => {
            const meta = ROOT_GROUP_META[root];
            const isActive = activeRoot === root;

            return (
              <button
                key={root}
                className={`catalog-root-card ${isActive ? 'catalog-root-card--active' : ''}`}
                type="button"
                onClick={() => handleSelectRoot(root)}
              >
                <span className="catalog-root-card__eyebrow">{root}</span>
                <span className="catalog-root-card__title">{meta.label}</span>
                <span className="catalog-root-card__description">{meta.description}</span>
              </button>
            );
          })}
        </section>

        <div className="catalog-layout">
          <article className="catalog-card">
            <div className="catalog-card__head">
              <div>
                <h2 className="catalog-card__title">Danh mục trong nhóm đang chọn</h2>
              </div>
              <div className="catalog-actions">
                <button className="catalog-btn catalog-btn--ghost" type="button" onClick={() => void reload()}>
                  Tải lại
                </button>
                <button className="catalog-btn catalog-btn--secondary" type="button" onClick={resetForm}>
                  Tạo mới
                </button>
              </div>
            </div>

            <div className="catalog-card__body">
              <div className="catalog-toolbar">
                <span className="catalog-chip">
                  {countVisibleCategories(activeGroup)} danh mục trong nhóm {getRootLabel(activeRoot)}
                </span>
                {activeGroup && (
                  <span className="catalog-chip catalog-chip--soft">
                    {countTreeNodes(activeGroup.root.children)} mục con
                  </span>
                )}
              </div>

              {status === 'loading' && <div className="catalog-state">Đang tải danh mục…</div>}
              {status === 'error' && <div className="catalog-state catalog-state--error">{error}</div>}

              {status === 'success' && !activeGroup && (
                <div className="catalog-empty">Nhóm này hiện chưa có dữ liệu.</div>
              )}

              {status === 'success' && activeGroup && (
                <div className="catalog-taxonomy catalog-taxonomy--single">
                  <section className="catalog-taxonomy__group">
                    <div className="catalog-taxonomy__head">
                      <div>
                        <div className="catalog-taxonomy__eyebrow">{activeGroup.root.name}</div>
                        <h3 className="catalog-taxonomy__title">{activeGroup.meta.label}</h3>
                        <p className="catalog-taxonomy__description">{activeGroup.meta.description}</p>
                      </div>
                      <span className="catalog-badge catalog-badge--info">
                        {countTreeNodes(activeGroup.root.children)} mục con
                      </span>
                    </div>
                    <div className="catalog-tree catalog-tree--scroller">
                      {activeGroup.root.children.length === 0 ? (
                        <div className="catalog-empty">
                          Nhóm này chưa có danh mục con. Chọn nhóm gốc để thêm tiếp.
                        </div>
                      ) : (
                        activeGroup.root.children.map((category) => (
                          <CategoryNode
                            key={category.categoryId}
                            category={category}
                            depth={1}
                            selectedCategoryId={selectedCategoryId}
                            onSelect={selectCategory}
                            onCreateChild={prepareCreateUnder}
                          />
                        ))
                      )}
                    </div>
                  </section>
                </div>
              )}
            </div>
          </article>

          <article className="catalog-card catalog-card--sticky">
            <div className="catalog-card__head">
              <div>
                <h2 className="catalog-card__title">
                  {selectedCategory ? `Chỉnh sửa ${selectedCategory.name}` : 'Tạo danh mục mới'}
                </h2>
              </div>
            </div>

            <div className="catalog-card__body">
              <form className="catalog-form" onSubmit={(event) => void handleSubmit(event)}>
                <div className="catalog-form__grid">
                  <div className="catalog-field">
                    <label htmlFor="category-taxonomy">Nhóm gốc</label>
                    <input
                      id="category-taxonomy"
                      value={activeGroup?.meta.label ?? activeRoot}
                      disabled
                      readOnly
                    />
                  </div>

                  <div className="catalog-field">
                    <label htmlFor="category-name">Tên danh mục</label>
                    <input
                      id="category-name"
                      value={form.name}
                      onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                      placeholder="Ví dụ: Toán THPT"
                      required
                    />
                  </div>

                  <div className="catalog-field">
                    <label htmlFor="category-parent">Danh mục cha</label>
                    <input
                      id="category-parent"
                      value={formatParentName(form.parentId, flatCategories, activeRoot)}
                      disabled
                      readOnly
                    />
                    <div className="catalog-form__hint">
                      <span className="catalog-badge catalog-badge--muted">
                        {selectedCategory
                          ? `Đang thuộc: ${formatParentName(form.parentId, flatCategories, activeRoot)}`
                          : `Tạo mới trong nhóm: ${formatParentName(form.parentId, flatCategories, activeRoot)}`}
                      </span>
                    </div>
                  </div>

                  <div className="catalog-field">
                    <label htmlFor="category-status">Trạng thái</label>
                    <select
                      id="category-status"
                      value={form.status}
                      onChange={(event) =>
                        setForm((current) => ({
                          ...current,
                          status: event.target.value as UpsertCategoryRequest['status'],
                        }))
                      }
                    >
                      <option value="ACTIVE">Đang hoạt động</option>
                      <option value="INACTIVE">Tạm ẩn</option>
                    </select>
                  </div>

                  <div className="catalog-field">
                    <label htmlFor="category-description">Mô tả</label>
                    <textarea
                      id="category-description"
                      value={form.description}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, description: event.target.value }))
                      }
                      placeholder="Mô tả ngắn để phân biệt danh mục này được dùng ở đâu."
                    />
                  </div>
                </div>

                {submitError && <div className="catalog-state catalog-state--error">{submitError}</div>}

                {selectedCategory && (
                  <div className="catalog-badges">
                    <span
                      className={`catalog-badge ${
                        selectedCategory.status === 'ACTIVE'
                          ? 'catalog-badge--active'
                          : 'catalog-badge--inactive'
                      }`}
                    >
                      {formatStatusLabel(selectedCategory.status)}
                    </span>
                    {selectedCategory.usedByTutorSubjects && (
                      <span className="catalog-badge catalog-badge--info">Đang dùng bởi hồ sơ gia sư</span>
                    )}
                    {selectedCategory.usedByTutoringClasses && (
                      <span className="catalog-badge catalog-badge--warn">Đang dùng bởi lớp học</span>
                    )}
                    {!selectedCategory.deletable && (
                      <span className="catalog-badge catalog-badge--muted">Không thể xóa</span>
                    )}
                  </div>
                )}

                <div className="catalog-form__footer">
                  <button className="catalog-btn catalog-btn--primary" type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Đang lưu…' : selectedCategory ? 'Cập nhật danh mục' : 'Tạo danh mục'}
                  </button>
                  <button className="catalog-btn catalog-btn--secondary" type="button" onClick={resetForm}>
                    Xóa form
                  </button>
                  {selectedCategory && (
                    <button
                      className="catalog-btn catalog-btn--danger"
                      type="button"
                      onClick={() => void handleDelete()}
                      disabled={isSubmitting || !selectedCategory.deletable}
                    >
                      Xóa danh mục
                    </button>
                  )}
                </div>
              </form>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}

function CategoryNode({
  category,
  depth,
  selectedCategoryId,
  onSelect,
  onCreateChild,
}: {
  category: CategoryItem;
  depth: number;
  selectedCategoryId: number | null;
  onSelect: (category: CategoryItem) => void;
  onCreateChild: (category: CategoryItem) => void;
}) {
  const isActive = selectedCategoryId === category.categoryId;
  const canCreateChild = depth === 1;

  return (
    <div className="catalog-node">
      <div className={`catalog-node__content ${isActive ? 'catalog-node__content--active' : ''}`}>
        <button className="catalog-node__mainAction" type="button" onClick={() => onSelect(category)}>
          <div className="catalog-node__main">
            <div className="catalog-node__title">
              <span className="catalog-node__name">{category.name}</span>
              <span
                className={`catalog-badge ${
                  category.status === 'ACTIVE' ? 'catalog-badge--active' : 'catalog-badge--inactive'
                }`}
              >
                {formatStatusLabel(category.status)}
              </span>
            </div>

            <div className="catalog-node__meta">
              {category.usedByTutorSubjects && (
                <span className="catalog-badge catalog-badge--info">Hồ sơ gia sư</span>
              )}
              {category.usedByTutoringClasses && (
                <span className="catalog-badge catalog-badge--warn">Lớp học</span>
              )}
              {!category.deletable && <span className="catalog-badge catalog-badge--muted">Không thể xóa</span>}
            </div>

            {category.description && <div className="catalog-node__description">{category.description}</div>}
          </div>
        </button>

        {canCreateChild && (
          <div className="catalog-node__actions">
            <button className="catalog-inline-btn" type="button" onClick={() => onCreateChild(category)}>
              Thêm
            </button>
          </div>
        )}
      </div>

      {category.children.length > 0 && (
        <div className="catalog-node__children">
          {category.children.map((child) => (
            <CategoryNode
              key={child.categoryId}
              category={child}
              depth={depth + 1}
              selectedCategoryId={selectedCategoryId}
              onSelect={onSelect}
              onCreateChild={onCreateChild}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function flattenCategories(categories: CategoryItem[]): CategoryItem[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children)]);
}

function countTreeNodes(categories: CategoryItem[]): number {
  return categories.reduce((total, category) => total + 1 + countTreeNodes(category.children), 0);
}

function extractUiError(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const maybeAxiosError = error as {
      response?: { data?: { message?: string } | Record<string, string> };
      message?: string;
    };

    const message = maybeAxiosError.response?.data;
    if (message && typeof message === 'object') {
      if ('message' in message && typeof message.message === 'string') {
        return message.message;
      }
      return Object.values(message).join(', ');
    }

    if (typeof maybeAxiosError.message === 'string') {
      return maybeAxiosError.message;
    }
  }

  return 'Không lưu được danh mục.';
}

function formatParentName(parentId: number | null, categories: CategoryItem[], root: RootGroup) {
  if (parentId == null) {
    return getRootLabel(root);
  }

  return categories.find((category) => category.categoryId === parentId)?.name ?? `#${parentId}`;
}

function formatStatusLabel(status: CategoryItem['status']) {
  return status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ẩn';
}

function getRootLabel(root: RootGroup) {
  return ROOT_GROUP_META[root].label;
}

function countVisibleCategories(activeGroup: { root: CategoryItem } | null) {
  if (!activeGroup) {
    return 0;
  }

  return countTreeNodes(activeGroup.root.children);
}
