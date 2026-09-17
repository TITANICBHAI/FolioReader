#!/usr/bin/env bash

set -euo pipefail

OLD_PACKAGE="${OLD_PACKAGE:-com.tbtechsdev.lexiread}"
NEW_PACKAGE="${NEW_PACKAGE:-com.tbtechs.folioreader}"
OLD_APPLICATION_ID="${OLD_APPLICATION_ID:-com.aistudio.folioreader.gvyrrw}"

OLD_PATH="${OLD_PACKAGE//./\/}"
NEW_PATH="${NEW_PACKAGE//./\/}"

if [[ "$OLD_PACKAGE" == "$NEW_PACKAGE" ]]; then
  echo "The old and new package names are identical." >&2
  exit 1
fi

rename_product_identifiers() {
  local main_root="app/src/main/java/$NEW_PATH"
  local test_root="app/src/test/java/$NEW_PATH"
  local app_file="$main_root/LexiReadApp.kt"
  local screen_file="$main_root/ui/LexiReadMainScreen.kt"
  local new_app_file="$main_root/FolioReaderApp.kt"
  local new_screen_file="$main_root/ui/FolioReaderMainScreen.kt"

  if [[ -f "$app_file" ]]; then
    if [[ -e "$new_app_file" ]]; then
      echo "Cannot rename $app_file: destination already exists at $new_app_file." >&2
      exit 1
    fi
    mv "$app_file" "$new_app_file"
  fi

  if [[ -f "$screen_file" ]]; then
    if [[ -e "$new_screen_file" ]]; then
      echo "Cannot rename $screen_file: destination already exists at $new_screen_file." >&2
      exit 1
    fi
    mv "$screen_file" "$new_screen_file"
  fi

  while IFS= read -r -d '' file; do
    perl -0pi -e '
      s/\bLexiReadApp\b/FolioReaderApp/g;
      s/\bLexiReadMainScreen\b/FolioReaderMainScreen/g;
      s/\bLexiReadTheme\b/FolioReaderTheme/g;
    ' "$file"
  done < <(
    printf '%s\0' \
      "$new_app_file" \
      "$new_screen_file" \
      "$main_root/MainActivity.kt" \
      "$main_root/ui/theme/Theme.kt" \
      "$test_root/GreetingScreenshotTest.kt" \
      "app/src/main/AndroidManifest.xml" |
      while IFS= read -r -d '' file; do
        [[ -f "$file" ]] && printf '%s\0' "$file"
      done
  )
}

if ! grep -Fq "$OLD_PACKAGE" app/build.gradle.kts 2>/dev/null && \
   ! rg -l --hidden --glob '!build/**' --glob '!.git/**' "$OLD_PACKAGE" app >/dev/null 2>&1; then
  if rg -q -F "$NEW_PACKAGE" app/build.gradle.kts 2>/dev/null; then
    rename_product_identifiers
    echo "Package rename already applied: $NEW_PACKAGE"
    exit 0
  fi
  echo "Could not find the expected old package: $OLD_PACKAGE" >&2
  exit 1
fi

replace_in_file() {
  local file="$1"
  OLD_PACKAGE="$OLD_PACKAGE" NEW_PACKAGE="$NEW_PACKAGE" \
    OLD_APPLICATION_ID="$OLD_APPLICATION_ID" perl -0pi -e '
      s/\Q$ENV{OLD_PACKAGE}\E/$ENV{NEW_PACKAGE}/g;
      s/\Q$ENV{OLD_APPLICATION_ID}\E/$ENV{NEW_PACKAGE}/g;
    ' "$file"
}

while IFS= read -r -d '' file; do
  replace_in_file "$file"
done < <(
  rg -l -0 --hidden \
    --glob '!build/**' \
    --glob '!.git/**' \
    --glob '!scripts/rename-package.sh' \
    "$OLD_PACKAGE|$OLD_APPLICATION_ID" \
    app build.gradle.kts settings.gradle.kts 2>/dev/null || true
)

for source_root in app/src/main/java app/src/test/java app/src/androidTest/java; do
  old_dir="$source_root/$OLD_PATH"
  new_dir="$source_root/$NEW_PATH"

  if [[ -d "$old_dir" ]]; then
    if [[ -e "$new_dir" ]]; then
      echo "Cannot move $old_dir: destination already exists at $new_dir." >&2
      exit 1
    fi
    mkdir -p "$(dirname "$new_dir")"
    mv "$old_dir" "$new_dir"
  fi
done

robolectric_test="app/src/test/java/$NEW_PATH/ExampleRobolectricTest.kt"
if [[ -f "$robolectric_test" ]]; then
  sed -i 's/assertEquals("LexiRead", appName)/assertEquals("Folio Reader", appName)/' \
    "$robolectric_test"
fi

rename_product_identifiers

echo "Renamed package to $NEW_PACKAGE."
echo "Application ID: $NEW_PACKAGE"
echo "Source paths now use: $NEW_PATH"