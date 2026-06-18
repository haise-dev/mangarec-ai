import { render } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import Register from "./Register";

vi.mock("./AuthPage", () => ({
  default: ({ type }: { type: string }) => <div data-testid={`auth-page-${type}`} />
}));

describe("Register", () => {
  it("renders AuthPage with type register", () => {
    const { getByTestId } = render(<Register />);
    expect(getByTestId("auth-page-register")).toBeInTheDocument();
  });
});
