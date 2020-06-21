let

  fetchNixpkgs = {rev, sha256}: builtins.fetchTarball {
    url = "https://github.com/NixOS/nixpkgs-channels/archive/${rev}.tar.gz";
    inherit sha256;
  };

  pkgs = import (fetchNixpkgs {
    rev = "9480bae337095fd24f61380bce3174fdfe926a00";
    sha256 = "1n5bnnral5w60kf68d9jvs7px1w3hx53d8pyg9yxkf1s2n3791j2";
  }) {};

  site =
    let theme = pkgs.fetchFromGitHub {
          owner = "alex-shpak";
          repo = "hugo-book";
          rev = "dae803fa442973561821a44b08e3a964614d07df";
          sha256 = "0dpb860kddclsqnr4ls356jn4d1l8ymw5rs9wfz2xq4kkrgls4dl";
        };
    in pkgs.stdenv.mkDerivation rec {
      name = "reno-site";
      src = ./doc/site;
      buildPhase = ''
        cp -a ${theme}/. themes/book
        ${pkgs.hugo}/bin/hugo --minify
      '';
      installPhase = ''
        cp -r public $out
      '';
    };

in

if pkgs.lib.inNixShell then pkgs.mkShell rec {

  buildInputs = with pkgs; [
    idea.idea-community emacs
    coursier sbt bloop ammonite
    hugo
  ];

  shellHooks = ''
    alias site="hugo server --port 7777 --bind 0.0.0.0 --source doc/site"
  '';
}
else {
  inherit site;
}
