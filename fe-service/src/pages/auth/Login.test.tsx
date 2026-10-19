import { render } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import Login from "./Login";

vi.mock("./AuthPage", () => ({
  default: ({ type }: { type: string }) => <div data-testid={`auth-page-${type}`} />
}));

describe("Login", () => {
  it("renders AuthPage with type login", () => {
    const { getByTestId } = render(<Login />);
    expect(getByTestId("auth-page-login")).toBeInTheDocument();
  });
});
