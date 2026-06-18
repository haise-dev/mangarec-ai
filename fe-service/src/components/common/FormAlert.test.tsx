import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { FormAlert } from "./FormAlert";

describe("FormAlert component", () => {
  it("should render error alert", () => {
    render(<FormAlert type="error">Error Message</FormAlert>);
    const alert = screen.getByText("Error Message");
    expect(alert).toBeInTheDocument();
    // In React testing library with jsdom, testing class of parent is better done by fetching the element directly if possible.
    // Or we can just get by text and check its class if the text is inside the container
    expect(alert).toHaveClass("form-alert", "form-alert--error");
  });

  it("should render success alert", () => {
    render(<FormAlert type="success">Success!</FormAlert>);
    const alert = screen.getByText("Success!");
    expect(alert).toHaveClass("form-alert--success");
  });
});
