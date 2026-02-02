import React from "react";
import { Button } from "@/components/ui/button";

interface Props {
    children: React.ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
    errorInfo: React.ErrorInfo | null;
}

export class ErrorBoundary extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error: Error) {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
        console.error("Uncaught error:", error, errorInfo);
        this.setState({ errorInfo });
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen flex flex-col items-center justify-center p-8 bg-background text-foreground">
                    <div className="max-w-2xl w-full bg-card border border-destructive/20 rounded-xl p-8 shadow-lg">
                        <h2 className="text-2xl font-bold text-destructive mb-4">Something went wrong</h2>
                        <p className="text-muted-foreground mb-6">
                            The application encountered an error. Please report this to the developer.
                        </p>

                        <div className="bg-secondary/50 p-4 rounded-lg overflow-auto max-h-[300px] mb-6 font-mono text-xs">
                            <p className="font-bold text-destructive">{this.state.error && this.state.error.toString()}</p>
                            <pre className="mt-2 text-muted-foreground opacity-70">
                                {this.state.errorInfo && this.state.errorInfo.componentStack}
                            </pre>
                        </div>

                        <Button
                            onClick={() => {
                                this.setState({ hasError: false, error: null, errorInfo: null });
                                window.location.reload();
                            }}
                        >
                            Reload Page
                        </Button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}
