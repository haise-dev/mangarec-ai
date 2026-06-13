"""SQLAlchemy ORM Models for MangaRec."""

from datetime import datetime
from typing import List, Optional

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class Manga(Base):
    """Core Manga metadata model."""

    __tablename__ = "mangas"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    title_main: Mapped[str] = mapped_column(String)
    title_original: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    original_language: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    description_en: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    description_vi: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    demographic: Mapped[Optional[str]] = mapped_column(String, nullable=True, index=True)
    status: Mapped[str] = mapped_column(String, index=True)
    content_rating: Mapped[str] = mapped_column(String, index=True)
    year: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    cover_url: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    rating_bayesian: Mapped[Optional[float]] = mapped_column(Float, nullable=True, index=True)
    rating_average: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    follows: Mapped[Optional[int]] = mapped_column(Integer, nullable=True, index=True)
    has_en_translation: Mapped[bool] = mapped_column(Boolean, default=False)
    has_vi_translation: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    updated_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)

    # Relationships
    alt_titles: Mapped[List["MangaAltTitle"]] = relationship(
        back_populates="manga", cascade="all, delete-orphan"
    )
    authors: Mapped[List["MangaAuthor"]] = relationship(
        back_populates="manga", cascade="all, delete-orphan"
    )
    tags: Mapped[List["MangaTag"]] = relationship(
        back_populates="manga", cascade="all, delete-orphan"
    )
    related: Mapped[List["RelatedManga"]] = relationship(
        foreign_keys="[RelatedManga.manga_id]", back_populates="manga", cascade="all, delete-orphan"
    )


class MangaAltTitle(Base):
    """Alternative titles for search indexing."""

    __tablename__ = "manga_alt_titles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    manga_id: Mapped[str] = mapped_column(ForeignKey("mangas.id"), index=True)
    title: Mapped[str] = mapped_column(String)
    language: Mapped[str] = mapped_column(String)

    manga: Mapped["Manga"] = relationship(back_populates="alt_titles")


class Author(Base):
    """Manga author/artist model."""

    __tablename__ = "authors"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    name: Mapped[str] = mapped_column(String)


class Tag(Base):
    """Manga tag model (genre, theme, format)."""

    __tablename__ = "tags"

    id: Mapped[str] = mapped_column(String, primary_key=True)
    name_en: Mapped[str] = mapped_column(String)
    group_type: Mapped[str] = mapped_column(String, index=True)


class MangaAuthor(Base):
    """Junction table mapping mangas to authors/artists."""

    __tablename__ = "manga_authors"

    manga_id: Mapped[str] = mapped_column(ForeignKey("mangas.id"), primary_key=True)
    author_id: Mapped[str] = mapped_column(ForeignKey("authors.id"), primary_key=True)
    role: Mapped[str] = mapped_column(String, primary_key=True)  # author or artist

    manga: Mapped["Manga"] = relationship(back_populates="authors")
    author: Mapped["Author"] = relationship()


class MangaTag(Base):
    """Junction table mapping mangas to tags."""

    __tablename__ = "manga_tags"

    manga_id: Mapped[str] = mapped_column(ForeignKey("mangas.id"), primary_key=True)
    tag_id: Mapped[str] = mapped_column(ForeignKey("tags.id"), primary_key=True)

    manga: Mapped["Manga"] = relationship(back_populates="tags")
    tag: Mapped["Tag"] = relationship()


class RelatedManga(Base):
    """Maps related mangas (sequels, spin-offs, etc)."""

    __tablename__ = "related_mangas"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    manga_id: Mapped[str] = mapped_column(ForeignKey("mangas.id"), index=True)
    related_manga_id: Mapped[str] = mapped_column(ForeignKey("mangas.id"), index=True)
    relation_type: Mapped[str] = mapped_column(String)

    manga: Mapped["Manga"] = relationship(foreign_keys=[manga_id], back_populates="related")

class EtlCheckpoint(Base):
    """Tracks ETL pipeline progress for resume capability."""

    __tablename__ = "etl_checkpoints"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    job_name: Mapped[str] = mapped_column(String, unique=True, index=True)
    last_offset: Mapped[int] = mapped_column(Integer, default=0)
    total_limit: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String)  # running, completed, failed
    started_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
