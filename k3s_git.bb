HOMEPAGE = "git://github.com/kubernetes/kubernetes"
SUMMARY = "Production-Grade Container Scheduling and Management"
DESCRIPTION = "Kubernetes is an open source system for managing containerized \
applications across multiple hosts, providing basic mechanisms for deployment, \
maintenance, and scaling of applications. \
"
PV = "v1.19+git${SRCREV_k3s}"
#SRCREV_k3s = "0064646628d2171a59c0bf2a815f434e61657c41"
SRC_URI = "git://github.com/rancher/k3s.git;branch=master;name=k3s \
		   file://cni-containerd-net.conf \
		   "
SRCREV_k3s = "0063646628d2171a59c0bf2a815f434e61657c41"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://src/import/LICENSE;md5=2ee41112a44fe7014dce33e26468ba93"

DEPENDS += " \
	    strongswan \
        curl \
        gzip \
        xz \
        tar \
		libseccomp \
"

RDEPENDS_${PN} += " \
		libseccomp \
"
RDEPENDS_${PN}-dev += " \
		bash \
"

GO_IMPORT = "import"

inherit systemd
inherit go
inherit goarch

BIN_PREFIX ?= "${exec_prefix}/local"

do_compile() {
    # Build the host tools first, using the host compiler
    export GOARCH="${BUILD_GOARCH}"
    # Pass the needed cflags/ldflags so that cgo can find the needed headers files and libraries
    export CGO_ENABLED="1"
    export CFLAGS="${BUILD_CFLAGS}"
    export LDFLAGS="${BUILD_LDFLAGS}"
    export CGO_CFLAGS="${BUILD_CFLAGS}"
    export CGO_LDFLAGS="${BUILD_LDFLAGS}"
    export CC="${BUILD_CC}"
    export LD="${BUILD_LD}"
    export PATH=${PATH}:/usr/bin
    cd ${S}/src/import
	mkdir -p build/data && ./scripts/download && go generate
    SKIP_VALIDATE=true oe_runmake CGO_FLAGS=${CGO_FLAGS} GO=${GO}  GOLDFLAGS=""
}

do_install() {
        install -d "${D}${BIN_PREFIX}/bin"
        install -m 755 "${S}/src/import/dist/artifacts/k3s-arm64" "${D}${BIN_PREFIX}/bin/k3s"
        ln -sr "${D}/${BIN_PREFIX}/bin/k3s" "${D}${BIN_PREFIX}/bin/crictl"
        ln -sr "${D}/${BIN_PREFIX}/bin/k3s" "${D}${BIN_PREFIX}/bin/ctr"
        ln -sr "${D}/${BIN_PREFIX}/bin/k3s" "${D}${BIN_PREFIX}/bin/kubectl"
		#install -m 755 "${WORKDIR}/k3s-clean" "${D}${BIN_PREFIX}/bin"
		install -D -m 0644 "${WORKDIR}/cni-containerd-net.conf" "${D}/${sysconfdir}/cni/net.d/10-containerd-net.conf"
        if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
                install -D -m 0644 "${S}/src/import/k3s.service" "${D}${systemd_system_unitdir}/k3s.service"
				#install -D -m 0644 "${WORKDIR}/k3s-agent.service" "${D}${systemd_system_unitdir}/k3s-agent.service"
				#sed -i "s#\(Exec\)\(.*\)=\(.*\)\(k3s\)#\1\2=${BIN_PREFIX}/bin/\4#g" "${D}${systemd_system_unitdir}/k3s.service" "${D}${systemd_system_unitdir}/k3s-agent.service"
				#install -m 755 "${WORKDIR}/k3s-agent" "${D}${BIN_PREFIX}/bin"
        fi
}

FILES_${PN} += " \
		${bindir}/* \
	    ${sbindir}/* \
		/usr/* \
		${libdir}/* \
		${libdir}/systemd/* \
		${libdir}/systemd/system/* \ 
	    ${systemd_unitdir}/* "
