import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { AuthLayout } from "./AuthLayout";
import { MemoryRouter } from "react-router-dom";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
  };
});

describe("AuthLayout", () => {
  it("renders correctly and handles navigation", () => {
    render(
      <MemoryRouter>
        <AuthLayout title="Test Title" eyebrow="Test Eyebrow" subtitle="Test Subtitle">
          <div>Test Children</div>
        </AuthLayout>
      </MemoryRouter>
    );

    expect(screen.getByText("Test Title")).toBeInTheDocument();
    expect(screen.getByText("Test Eyebrow")).toBeInTheDocument();
    expect(screen.getByText("Test Subtitle")).toBeInTheDocument();
    expect(screen.getByText("Test Children")).toBeInTheDocument();

    const exploreBtn = screen.getByText("Khám phá trước");
    fireEvent.click(exploreBtn);

    expect(mockNavigate).toHaveBeenCalledWith("/");
  });
});
