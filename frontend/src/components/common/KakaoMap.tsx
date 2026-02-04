import { Map, MapMarker, useKakaoLoader } from "react-kakao-maps-sdk";
import { useEffect, useState, useCallback } from "react";
import { Loader2, MapPin, AlertCircle } from "lucide-react";

interface KakaoMapProps {
    address?: string;
    latitude?: number;
    longitude?: number;
    companyName?: string;
}

// 카카오맵 타입 선언
declare global {
    interface Window {
        kakao: any;
    }
}

export function KakaoMap({ address, latitude, longitude, companyName }: KakaoMapProps) {
    const apiKey = import.meta.env.VITE_KAKAO_MAP_API_KEY || "";
    
    const [loading, error] = useKakaoLoader({
        appkey: apiKey,
        libraries: ["services", "clusterer"],
    });

    // Default to Seoul City Hall if no coordinates
    const [center, setCenter] = useState({ lat: 37.5665, lng: 126.9780 });
    const [isGeocoding, setIsGeocoding] = useState(false);
    const [geocodeError, setGeocodeError] = useState<string | null>(null);
    const [isMapReady, setIsMapReady] = useState(false);

    // 주소를 좌표로 변환
    const geocodeAddress = useCallback((addressString: string) => {
        if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
            console.warn("카카오맵 서비스가 아직 로드되지 않았습니다.");
            return;
        }

        setIsGeocoding(true);
        setGeocodeError(null);

        const geocoder = new window.kakao.maps.services.Geocoder();

        // 주소로 좌표 검색
        geocoder.addressSearch(addressString, (result: any[], status: string) => {
            if (status === window.kakao.maps.services.Status.OK && result.length > 0) {
                const coords = {
                    lat: parseFloat(result[0].y),
                    lng: parseFloat(result[0].x),
                };
                setCenter(coords);
                setIsMapReady(true);
            } else {
                // 주소 검색 실패 시 키워드 검색 시도
                const places = new window.kakao.maps.services.Places();
                places.keywordSearch(addressString, (placeResult: any[], placeStatus: string) => {
                    if (placeStatus === window.kakao.maps.services.Status.OK && placeResult.length > 0) {
                        const coords = {
                            lat: parseFloat(placeResult[0].y),
                            lng: parseFloat(placeResult[0].x),
                        };
                        setCenter(coords);
                        setIsMapReady(true);
                    } else {
                        console.warn("주소 변환 실패:", addressString);
                        setGeocodeError("주소를 찾을 수 없습니다");
                        setIsMapReady(true); // 기본 위치로 표시
                    }
                    setIsGeocoding(false);
                });
            }
            setIsGeocoding(false);
        });
    }, []);

    useEffect(() => {
        // API 키가 없으면 에러 표시
        if (!apiKey) {
            setGeocodeError("카카오맵 API 키가 설정되지 않았습니다");
            setIsMapReady(true);
            return;
        }

        // 직접 좌표가 전달된 경우
        if (latitude && longitude) {
            setCenter({ lat: latitude, lng: longitude });
            setIsMapReady(true);
            return;
        }

        // 주소가 있고 카카오맵이 로드되면 지오코딩
        if (address && !loading && !error && window.kakao?.maps?.services) {
            geocodeAddress(address);
        } else if (!address) {
            setIsMapReady(true);
        }
    }, [address, latitude, longitude, loading, error, apiKey, geocodeAddress]);

    // 카카오맵 로드 에러
    if (error) {
        return (
            <div className="w-full h-[300px] rounded-xl overflow-hidden border border-slate-200 shadow-sm bg-slate-50 flex flex-col items-center justify-center gap-2 text-slate-500">
                <AlertCircle className="h-8 w-8 text-amber-500" />
                <p className="text-sm font-medium">지도를 불러올 수 없습니다</p>
                <p className="text-xs text-slate-400">카카오맵 API 설정을 확인해주세요</p>
            </div>
        );
    }

    // 로딩 중
    if (loading || isGeocoding || !isMapReady) {
        return (
            <div className="w-full h-[300px] rounded-xl overflow-hidden border border-slate-200 shadow-sm bg-slate-50 flex flex-col items-center justify-center gap-2">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                <p className="text-sm text-slate-500">지도를 불러오는 중...</p>
            </div>
        );
    }

    return (
        <div className="w-full h-[300px] rounded-xl overflow-hidden border border-slate-200 shadow-sm relative bg-slate-100">
            {geocodeError && (
                <div className="absolute top-2 left-2 right-2 z-10 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 flex items-center gap-2">
                    <MapPin className="h-4 w-4 text-amber-600 flex-shrink-0" />
                    <span className="text-xs text-amber-700">{geocodeError} - 기본 위치를 표시합니다</span>
                </div>
            )}
            <Map
                center={center}
                style={{ width: "100%", height: "100%" }}
                level={3}
            >
                <MapMarker position={center}>
                    {companyName && (
                        <div style={{ padding: "8px 12px", color: "#000", minWidth: "120px" }}>
                            <strong>{companyName}</strong>
                            {address && (
                                <div style={{ fontSize: "11px", color: "#666", marginTop: "4px" }}>
                                    {address}
                                </div>
                            )}
                        </div>
                    )}
                </MapMarker>
            </Map>
        </div>
    );
}
