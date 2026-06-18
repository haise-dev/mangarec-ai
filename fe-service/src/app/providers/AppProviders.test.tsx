import { render } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { AppProviders } from "./AppProviders";

describe("AppProviders", () => {
  it("should render children correctly", () => {
    const { getByText } = render(
      <AppProviders>
        <div>Test Child</div>
      </AppProviders>
    );
    expect(getByText("Test Child")).toBeInTheDocument();
  });
});
