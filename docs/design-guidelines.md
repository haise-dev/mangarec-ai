# MangaRec AI — Design System & Frontend Guidelines

> **Loại tài liệu:** Design System & Frontend Development Guide  
> **Đối tượng:** Frontend Developer, UI/UX Designer  
> **Cập nhật lần cuối:** 2026-06-15  
> **Mục tiêu:** Đảm bảo giao diện nhất quán, đẹp mắt, dễ bảo trì

---

## 1. Design Philosophy (Triết lý Thiết kế)

- **Aesthetic:** Dark mode-first, modern, premium — lấy cảm hứng từ các nền tảng streaming.
- **Core Values:** Dễ khám phá (Discoverable) · Phản hồi nhanh (Responsive) · Tập trung vào nội dung (Content-first)
- **Target feel:** "AI-powered, but human"

---

## 2. Color System (Hệ thống Màu sắc Thực tế)

Tất cả màu được định nghĩa trong `fe-service/src/styles/index.css` dưới dạng CSS custom properties:

```css
:root {
  --bg:           #0f1117;  /* Background chính — dark base */
  --bg-soft:      #151925;  /* Background mềm — panels, cards */
  --surface:      #f7f4ee;  /* Surface sáng (contrast element) */
  --surface-strong: #ffffff;
  --ink:          #17181d;  /* Text trên surface sáng */
  --muted:        #6b6874;  /* Text phụ, placeholder, caption */
  --line:         rgba(23, 24, 29, 0.12);   /* Border trên surface sáng */
  --line-dark:    rgba(255, 255, 255, 0.14); /* Border trên dark bg */
  --blue:         #5b8def;  /* Primary CTA, links, highlights */
  --blue-strong:  #2f66d5;  /* Primary CTA hover/active state */
  --green:        #2bc487;  /* Success, trạng thái thành công */
  --coral:        #ff6b4a;  /* Warning, accent */
  --gold:         #ffce5c;  /* Secondary accent, badges, ratings */
  --red:          #c9473f;  /* Error, destructive actions */
}
```

### Background Gradient (Body)
Body dùng radial gradient đa lớp tạo depth:
```css
background:
  radial-gradient(circle at 15% 8%, rgba(91, 141, 239, 0.24), transparent 28rem),  /* Blue top-left */
  radial-gradient(circle at 82% 12%, rgba(43, 196, 135, 0.18), transparent 24rem), /* Green top-right */
  radial-gradient(circle at 56% 90%, rgba(255, 206, 92, 0.16), transparent 32rem), /* Gold bottom */
  var(--bg);
```

### Sử dụng màu
| Token | Dùng cho |
|---|---|
| `--bg` | Background toàn trang |
| `--bg-soft` | Card nền, panel, sidebar |
| `--surface` / `--surface-strong` | Content area sáng, form fields |
| `--ink` | Text chính trên nền sáng |
| `--muted` | Caption, placeholder, label phụ |
| `--blue` | Primary button, link, highlight |
| `--blue-strong` | Button hover/active |
| `--green` | Success toast, check icon |
| `--coral` | Warning badge, alert |
| `--gold` | Rating stars, premium badge |
| `--red` | Error message, delete action |
| `--line-dark` | Divider, border trên dark background |

---

## 3. Typography (Kiểu chữ)

### Font Family (thực tế từ `index.css`)
```css
font-family: "Google Sans Text", "Aptos", "Segoe UI", sans-serif;
```
Font được import từ Google Fonts:
```css
@import url("https://fonts.googleapis.com/css2?family=Google+Sans+Text:wght@400;500;700;800&display=swap");
```

> **Lưu ý:** Dự án dùng **Google Sans Text** (không phải Inter/Outfit như plan.md mô tả ban đầu).

### Type Scale (đề xuất — dựa trên plan.md, cần chuẩn hoá vào CSS)
| Token | Value | Dùng cho |
|---|---|---|
| `text-xs` | 0.75rem (12px) | Caption, label phụ |
| `text-sm` | 0.875rem (14px) | Secondary text |
| `text-base` | 1rem (16px) | Body text (default) |
| `text-lg` | 1.125rem (18px) | Sub-heading |
| `text-xl` | 1.25rem (20px) | Card title |
| `text-2xl` | 1.5rem (24px) | Section heading |
| `text-3xl` | 1.875rem (30px) | Page title |

---

## 4. Animations (Hệ thống Animation Thực tế)

Animations được định nghĩa trong `fe-service/src/styles/animations.css`:

```css
/* Panel floating animation — dùng cho hero/decorative elements */
@keyframes panelDrift {
  0%   { transform: translate3d(0, 0, 0) rotate(-8deg); }
  50%  { transform: translate3d(12px, -18px, 0) rotate(-3deg); }
  100% { transform: translate3d(0, 0, 0) rotate(-8deg); }
}

/* Content entrance animation */
@keyframes riseIn {
  from { opacity: 0; transform: translateY(18px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Pulsing orb / loading indicator */
@keyframes pulseOrb {
  0%, 100% { transform: scale(0.86); opacity: 0.58; }
  50%       { transform: scale(1.1); opacity: 1; }
}
```

### Nguyên tắc Animation
- **Timing micro-interactions:** 150–200ms
- **Page transitions / entrance:** 300ms
- **Easing:** `ease-out` cho enter, `ease-in` cho exit
- **Loading states:** Dùng `pulseOrb` hoặc skeleton loader — mọi async action phải có indicator
- **Hover effects:** Scale 1.02 cho cards, color shift cho buttons/links
- **Không dùng:** Animation quá phức tạp gây mất tập trung vào nội dung

---

## 5. Spacing & Layout

### Spacing System (Tailwind-based, bội số 4px)
| Token | Value |
|---|---|
| `xs` | 4px (1 unit) |
| `sm` | 8px (2 units) |
| `md` | 16px (4 units) |
| `lg` | 24px (6 units) |
| `xl` | 32px (8 units) |
| `2xl` | 48px (12 units) |

### Layout Grid
- **Max width:** 1280px
- **Padding:** 16px (mobile) / 24px (tablet) / 32px (desktop)
- **Columns:** 12-column grid

### Breakpoints (Tailwind defaults)
| Token | Value |
|---|---|
| `sm` | 640px |
| `md` | 768px |
| `lg` | 1024px |
| `xl` | 1280px |

---

## 6. Component Library

### 6.1 Components Đã Có (từ codebase)

| File | Location | Mô tả |
|---|---|---|
| `ChatPanel.tsx` | `components/chat/` | Chat UI panel |
| `FormAlert.tsx` | `components/common/` | Alert/error message trong form |
| `GoogleSignInButton.tsx` | `components/common/` | Google OAuth button |
| `Loading.tsx` | `components/common/` | Loading spinner/indicator |
| `PasswordField.tsx` | `components/common/` | Password input với toggle visibility |

### 6.2 Component Organization (Convention)
```
src/components/
├── common/    # Button, Input, Modal, Loading, FormAlert — dùng mọi nơi
├── manga/     # MangaCard, MangaGrid, MangaDetail
├── chat/      # ChatPanel, ChatBubble, ChatInput
├── auth/      # LoginForm, RegisterForm (hiện ở pages/auth/)
└── layout/    # Navbar, Sidebar, Footer
```

### 6.3 Naming Convention
Pattern: `[Feature][Component].tsx`

| Loại | Convention | Ví dụ |
|---|---|---|
| Component | `PascalCase.tsx` | `MangaCard.tsx`, `ChatPanel.tsx` |
| Hook | `useCamelCase.ts` | `useAuth.ts` |
| Utility | `camelCase.ts` | `formatDate.ts` |

### 6.4 Component Specs (Roadmap)

**Buttons:**
- Variants: Primary, Secondary, Ghost, Danger
- Sizes: sm / md / lg
- States: Default, Hover, Loading, Disabled

**Cards:**
- `MangaCard` — cover + title + tags + rating
- `SkeletonCard` — loading placeholder (CSS skeleton animation)

**Forms:**
- Input, Textarea, Select, Checkbox, Toggle
- States: Default, Focus, Error, Success
- Validation hiển thị qua `FormAlert` component

**Navigation:**
- Navbar (desktop) / Bottom bar (mobile)
- Breadcrumb

**Feedback:**
- Toast notifications
- Modal/Dialog
- Alert Banner
- Spinner / Loading

**AI-specific:**
- `ChatPanel` (đã có) — chat window
- ChatBubble (user / bot variant)
- TypingIndicator
- StreamingText

---

## 7. Accessibility (Khả năng Tiếp cận)

- **Tỷ lệ tương phản:** Tối thiểu WCAG AA (4.5:1 cho text thường, 3:1 cho text lớn)
- **Focus visible:** Mọi interactive element phải có `:focus-visible` style rõ ràng
- **Alt text:** Tất cả `<img>` phải có `alt` mô tả
- **Form labels:** Mọi input phải có `<label>` liên kết đúng (`htmlFor` + `id`)
- **ARIA:** Dùng ARIA roles/attributes cho custom components (modal, dropdown, tooltip)

---

## 8. Styling Rules

### Quy tắc Bắt buộc
1. **Dùng Tailwind CSS v4** utility classes là ưu tiên đầu tiên
2. **Dùng CSS variables** (`--blue`, `--bg`, v.v.) thay vì hardcode giá trị màu
3. **Không viết inline styles** trừ dynamic values không thể express bằng Tailwind
4. Custom CSS (khi Tailwind không đủ) đặt trong `src/styles/` — `index.css` (global) hoặc file riêng

### File Structure (`src/styles/`)
```
styles/
├── index.css       # Entry point: CSS variables, body styles, global resets
└── animations.css  # @keyframes definitions (imported vào index.css)
```

### Vite Path Alias
Import component dùng `@/` thay vì relative path:
```typescript
// ✅ Đúng
import { Loading } from "@/components/common/Loading";

// ❌ Tránh (nếu path dài)
import { Loading } from "../../../components/common/Loading";
```

---

## 9. Context & State

### Contexts Đã Có
- `AuthContext.tsx` — Authentication state (user, token, login/logout)
- `auth-context-value.ts` — Type definitions cho AuthContext

### Hooks Đã Có
- `useAuth.ts` — Hook consume `AuthContext`

### Pages Đã Có
```
pages/
├── auth/       # Login, Register, Verify Email, Reset Password
├── dashboard/  # User dashboard
└── home/       # Landing page
```

---

> **Liên kết liên quan:**
> - Code standards chi tiết → `docs/code-standards.md`
> - Cấu trúc codebase → `docs/codebase-summary.md`
