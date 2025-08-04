class Aimatrix < Formula
  desc "Aimatrix Master Agent - Advanced Language Server Protocol client for Kotlin"
  homepage "https://github.com/aimatrix/amx-mcp-lsp-kt"
  url "https://github.com/aimatrix/amx-mcp-lsp-kt/releases/download/v1.0.3/aimatrix-1.0.3.dmg"
  sha256 "7403c79e0277771a1d8f11d69f9bb64d81bb240fd95f4a85f961ad5be7803541"

  depends_on :macos

  def install
    # Extract the app from the DMG
    system "hdiutil", "attach", "-nobrowse", cached_download, "-mountpoint", buildpath/"mount"

    # Copy the app to a temporary location first
    app = buildpath/"mount/aimatrix.app"
    cp_r app, buildpath/"aimatrix.app"
    
    # Install from the copy
    prefix.install buildpath/"aimatrix.app"

    # Create a command-line launcher named 'aimatrix'
    (bin/"aimatrix").write <<~EOS
      #!/bin/bash
      exec "#{prefix}/aimatrix.app/Contents/MacOS/aimatrix" "$@"
    EOS
    (bin/"aimatrix").chmod 0755

    # Unmount the DMG
    system "hdiutil", "detach", buildpath/"mount"
  end

  def caveats
    <<~EOS
      AIMatrix has been installed.

      You can launch it from the command line with:
        aimatrix

      Or find it in your Applications folder after running:
        open #{prefix}/aimatrix.app
    EOS
  end

  test do
    # Test that the application can be launched
    assert_path_exists prefix/"aimatrix.app"
    assert_path_exists bin/"aimatrix"
  end
end
