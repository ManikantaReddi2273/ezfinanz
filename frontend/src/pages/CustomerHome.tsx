import { useAuth } from "../auth/AuthContext";

export function CustomerHome() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-cream">
      <header className="bg-navy text-cream px-6 py-4 flex items-center justify-between">
        <p className="text-lg font-semibold">EZFINANZ</p>
        <button type="button" onClick={logout} className="text-sm text-cream/80 hover:text-white">
          Sign out
        </button>
      </header>
      <main className="mx-auto max-w-3xl p-6">
        <h1 className="text-2xl font-semibold text-navy">Customer dashboard</h1>
        <p className="mt-2 text-stone-600">
          Signed in as {user?.fullName || user?.email || user?.phone}.
        </p>
        <div className="mt-6 rounded-xl border border-stone-200 bg-white p-5">
          <p className="text-sm font-medium text-teal">Email and phone verified</p>
          <ul className="mt-3 space-y-2 text-sm text-navy">
            <li>Email: {user?.email}</li>
            <li>Phone: {user?.phone}</li>
          </ul>
          <p className="mt-6 text-stone-600">Next step: KYC. That will be added in the next phase.</p>
        </div>
      </main>
    </div>
  );
}
