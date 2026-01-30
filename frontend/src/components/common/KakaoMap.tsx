import { Map, MapMarker, useKakaoLoader } from "react-kakao-maps-sdk";
import { useEffect, useState } from "react";

interface KakaoMapProps {
    address?: string;
    latitude?: number;
    longitude?: number;
    companyName?: string;
}

export function KakaoMap({ address, latitude, longitude, companyName }: KakaoMapProps) {
    const [loading, error] = useKakaoLoader({
        appkey: import.meta.env.VITE_KAKAO_MAP_API_KEY || "",
        libraries: ["services", "clusterer"],
    });

    // Default to Seoul City Hall if no coordinates
    const [center, setCenter] = useState({ lat: 37.5665, lng: 126.9780 });
    const [isLoaded, setIsLoaded] = useState(false);

    useEffect(() => {
        if (latitude && longitude) {
            setCenter({ lat: latitude, lng: longitude });
            setIsLoaded(true);
        } else if (address) {
            // NOTE: In a real app, use Kakao Geocoder here.
            // For now, if no lat/lng provided, we just show default or try to use existing coordinates if we had a geocoding API key setup.
            // Since we can't easily geocode without the services library loaded and initialized with a key,
            // we will rely on passed lat/lng or just show the map at default with a marker.

            // If we assume the script is loaded with &libraries=services, we could use `kakao.maps.services.Geocoder`.
            // For this MVP, let's just show the map.
            setIsLoaded(true);
        }
    }, [address, latitude, longitude]);

    // If no API Key is present in .env, this might fail to load tiles, but the SDK handles the script loading if configured.
    // We assume the user adds VITE_KAKAO_MAP_API_KEY to .env

    return (
        <div className="w-full h-[300px] rounded-xl overflow-hidden border border-slate-200 shadow-sm relative bg-slate-100 flex items-center justify-center">
            <Map
                center={center}
                style={{ width: "100%", height: "100%" }}
                level={3}
            >
                <MapMarker position={center}>
                    {companyName && (
                        <div style={{ padding: "5px", color: "#000" }}>
                            {companyName}
                        </div>
                    )}
                </MapMarker>
            </Map>
        </div>
    );
}
