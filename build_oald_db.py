#!/usr/bin/env python3
import zipfile
import json
import sqlite3
import os
import re

def normalize_term(term):
    if not term:
        return ""
    t = term.lower().replace("·", "")
    t = re.sub(r"[“”\"'’‘.,;:!?\(\)\[\]{}«»—–\-]+", " ", t)
    t = re.sub(r"\s+", " ", t).strip()
    return t

def parse_cross_ref_note(note):
    # e.g. "mis·took PAST TENSE OF mistake" or "took PAST TENSE OF take"
    m = re.search(r"([A-Z\s]+(?:OF|TO))\s+([a-zA-Z\s·\-]+)", note)
    if m:
        label = m.group(1).strip()
        base = m.group(2).replace("·", "").strip()
        return {"label": label, "base": base}
    return None

def build_database(zip_path, output_db_path):
    os.makedirs(os.path.dirname(output_db_path), exist_ok=True)
    if os.path.exists(output_db_path):
        os.remove(output_db_path)

    print(f"Reading {zip_path}...")
    with zipfile.ZipFile(zip_path) as z:
        with z.open("entries.json") as f:
            raw_entries = json.load(f)

    print(f"Loaded {len(raw_entries)} raw entries. Processing...")

    conn = sqlite3.connect(output_db_path)
    cur = conn.cursor()

    cur.execute("PRAGMA journal_mode = OFF;")
    cur.execute("PRAGMA synchronous = 0;")
    cur.execute("CREATE TABLE entries (headword TEXT PRIMARY KEY, json TEXT);")
    cur.execute("CREATE TABLE words (term TEXT, headword TEXT);")

    inserted_entries = 0
    words_pairs = set()

    for item in raw_entries:
        raw_hw = item.get("headword") or ""
        if not raw_hw:
            continue
        clean_hw = raw_hw.replace("·", "").strip()
        if not clean_hw:
            continue

        pronunciation = item.get("pronunciation")
        pos = item.get("part_of_speech") or "general"
        origin = item.get("origin")
        raw_defs = item.get("definitions") or []
        raw_senses = item.get("senses") or []
        raw_examples = item.get("examples") or []
        raw_notes = item.get("notes") or []
        raw_inflections = item.get("inflections") or []

        # Collect definitions
        definitions = []
        for d in raw_defs:
            if isinstance(d, str) and d.strip():
                definitions.append(d.strip())
        
        # If no definitions in raw_defs, check senses
        if not definitions and raw_senses:
            for s in raw_senses:
                if isinstance(s, dict) and s.get("definition"):
                    definitions.append(s["definition"].strip())

        # Collect examples
        examples = []
        for ex in raw_examples:
            if isinstance(ex, dict):
                eng = ex.get("english")
                if eng and isinstance(eng, str) and eng.strip():
                    examples.append(eng.strip())
            elif isinstance(ex, str) and ex.strip():
                examples.append(ex.strip())

        # Check cross reference notes if definitions are empty
        cross_ref = None
        if not definitions and raw_notes:
            for note in raw_notes:
                if isinstance(note, str):
                    cr = parse_cross_ref_note(note)
                    if cr:
                        cross_ref = cr
                        break

        # Construct group
        groups = []
        if definitions:
            groups.append({
                "part_of_speech": pos,
                "definitions": definitions,
                "examples": examples[:8]
            })

        entry_obj = {
            "headword": clean_hw,
            "pronunciation": pronunciation,
            "groups": groups
        }
        if origin:
            entry_obj["origin"] = origin
        if cross_ref:
            entry_obj["cross_ref"] = cross_ref

        json_str = json.dumps(entry_obj, separators=(',', ':'), ensure_ascii=False)

        # Unique key in entries table: we can use clean_hw or raw_hw.
        # Since some headwords might have multiple entries (e.g. homographs),
        # let's make the primary key clean_hw (or unique handle if duplicate).
        entry_key = clean_hw
        # Check if already inserted, if so, merge or distinguish
        cur.execute("SELECT json FROM entries WHERE headword = ?", (entry_key,))
        existing = cur.fetchone()
        if existing:
            # Merge groups
            try:
                ex_obj = json.loads(existing[0])
                ex_groups = ex_obj.get("groups", [])
                if groups:
                    ex_groups.extend(groups)
                ex_obj["groups"] = ex_groups
                json_str = json.dumps(ex_obj, separators=(',', ':'), ensure_ascii=False)
                cur.execute("UPDATE entries SET json = ? WHERE headword = ?", (json_str, entry_key))
            except Exception:
                pass
        else:
            cur.execute("INSERT INTO entries (headword, json) VALUES (?, ?)", (entry_key, json_str))
            inserted_entries += 1

        # Add index terms
        norm_hw = normalize_term(clean_hw)
        if norm_hw:
            words_pairs.add((norm_hw, entry_key))
            words_pairs.add((clean_hw.lower(), entry_key))

        for inf in raw_inflections:
            if isinstance(inf, str):
                norm_inf = normalize_term(inf)
                if norm_inf:
                    words_pairs.add((norm_inf, entry_key))
                clean_inf = inf.replace("·", "").strip().lower()
                if clean_inf:
                    words_pairs.add((clean_inf, entry_key))

        if cross_ref:
            norm_base = normalize_term(cross_ref["base"])
            if norm_base:
                words_pairs.add((norm_base, entry_key))

    print(f"Inserting {len(words_pairs)} word-index pairs into SQLite...")
    cur.executemany("INSERT INTO words (term, headword) VALUES (?, ?)", list(words_pairs))

    print("Creating index idx_words_term...")
    cur.execute("CREATE INDEX idx_words_term ON words (term);")
    cur.execute("CREATE INDEX idx_entries_hw ON entries (headword);")

    conn.commit()
    conn.close()

    db_size = os.path.getsize(output_db_path) / (1024 * 1024)
    print(f"Finished! SQLite DB created at {output_db_path} ({db_size:.2f} MB)")

if __name__ == "__main__":
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/assets/dictionaries/oald10.db")
    zip_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "OALD-10.zip")
    build_database(zip_path, output_path)
