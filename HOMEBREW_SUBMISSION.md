# Homebrew Submission Guide for AIMatrix

## Prerequisites Completed ✅

1. **GitHub Release Created**: v1.0.3
   - URL: https://github.com/aimatrix/amx-mcp-lsp-kt/releases/tag/v1.0.3
   - Asset: aimatrix-1.0.3.dmg
   - SHA256: e3ee244b26791c2377cf6e2fa8af658ea607ec4c0a928837f2da9e63f0e49b3b

2. **Homebrew Formula Created**: `homebrew/aimatrix.rb`
   - Program name: `aimatrix` (command-line)
   - App name: aimatrix.app (GUI)
   - Formula tested locally

## Steps to Submit to Homebrew

### Option 1: Submit to Homebrew Core (Recommended for wide distribution)

1. Fork the homebrew-core repository:
   ```bash
   gh repo fork homebrew/homebrew-core
   ```

2. Create a new branch:
   ```bash
   cd homebrew-core
   git checkout -b aimatrix-1.0.3
   ```

3. Copy the formula:
   ```bash
   cp /path/to/amx-mcp-lsp-kt/homebrew/aimatrix.rb Formula/
   ```

4. Test the formula:
   ```bash
   brew install --build-from-source Formula/aimatrix.rb
   brew test aimatrix
   brew audit --new-formula aimatrix
   ```

5. Commit and push:
   ```bash
   git add Formula/aimatrix.rb
   git commit -m "aimatrix 1.0.3 (new formula)"
   git push origin aimatrix-1.0.3
   ```

6. Create pull request:
   ```bash
   gh pr create --title "aimatrix 1.0.3 (new formula)" \
     --body "New formula for AIMatrix - Advanced Language Server Protocol client for Kotlin"
   ```

### Option 2: Create Your Own Tap (Easier, immediate availability)

1. Create a new repository named `homebrew-aimatrix` on GitHub

2. Add the formula:
   ```bash
   mkdir Formula
   cp homebrew/aimatrix.rb Formula/
   git add .
   git commit -m "Add aimatrix formula"
   git push
   ```

3. Users can then install with:
   ```bash
   brew tap aimatrix/aimatrix
   brew install aimatrix
   ```

## Testing Commands

```bash
# Test installation
brew install local/test/aimatrix

# Test the command-line launcher
aimatrix

# Test the formula
brew test local/test/aimatrix

# Audit the formula
brew audit --new-formula local/test/aimatrix
```

## Notes

- The formula installs the app to Homebrew's Cellar
- Creates a command-line launcher named `aimatrix`
- The GUI app can still be accessed via the .app bundle
- Formula is macOS-only due to the DMG packaging

## Future Improvements

1. Add version detection from the app itself
2. Consider creating a cask formula for better macOS app integration
3. Add support for other platforms (Linux, Windows) when available