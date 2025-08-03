class Aimatrix < Formula
  desc "Advanced Language Server Protocol client for Kotlin"
  homepage "https://github.com/aimatrix/amx-mcp-lsp-kt"
  url "https://github.com/aimatrix/amx-mcp-lsp-kt/releases/download/v1.0.3/AmxLSP-1.0.3.dmg"
  sha256 "e3ee244b26791c2377cf6e2fa8af658ea607ec4c0a928837f2da9e63f0e49b3b"

  depends_on :macos

  def install
    # Extract the app from the DMG
    system "hdiutil", "attach", "-nobrowse", cached_download, "-mountpoint", buildpath/"mount"

    # Copy the app to a temporary location first
    app = buildpath/"mount/AmxLSP.app"
    cp_r app, buildpath/"AmxLSP.app"
    
    # Install from the copy
    prefix.install buildpath/"AmxLSP.app"

    # Create a command-line launcher named 'aimatrix'
    (bin/"aimatrix").write <<~EOS
      #!/bin/bash
      exec "#{prefix}/AmxLSP.app/Contents/MacOS/AmxLSP" "$@"
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
        open #{prefix}/AmxLSP.app
    EOS
  end

  test do
    # Test that the application can be launched
    assert_path_exists prefix/"AmxLSP.app"
    assert_path_exists bin/"aimatrix"
  end
end
