import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { Loading } from "./Loading";

describe("Loading component", () => {
  it("should render loading screen", () => {
    render(<Loading />);
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText("Đang tải MangaRec...")).toBeInTheDocument();
  });
});
