import requests
import json
import sys

BASE_URL = "http://localhost:8080"
ACCOUNT = "ACC123456"
ISBN = "9783333333333"  # Clean Code
BOOK_IDS = [f"{ISBN}-0", f"{ISBN}-1"]

# Track test results
results = []

def assert_success(response, label, must_have_json=True):
    success = response.status_code == 200
    if must_have_json:
        try:
            response.json()
        except Exception:
            success = False

    print(f"\n🔹 {label}")
    print(f"{response.request.method} {response.url}")
    if response.request.body:
        print("Request Body:", response.request.body)
    print("Status Code:", response.status_code)

    if success:
        print("✅ SUCCESS")
    else:
        print("❌ FAILURE")
        try:
            print("Response:", response.json())
        except Exception:
            print("Response Text:", response.text)

    print("=" * 80)
    results.append((label, success))

def get_user():
    return requests.get(f"{BASE_URL}/user/{ACCOUNT}")

def get_catalog_by_isbn(isbn):
    return requests.get(f"{BASE_URL}/catalog/isbn/{isbn}")

def check_out(book_ids):
    return requests.post(f"{BASE_URL}/activity/checkout", json={
        "accountNumber": ACCOUNT,
        "bookIds": book_ids
    })

def check_in(book_ids):
    return requests.post(f"{BASE_URL}/activity/checkin", json={
        "bookIds": book_ids
    })

def smoke_test():
    print("📘 Smoke Test: Library API (User: Alice Smith)\n")

    # Initial state
    assert_success(get_user(), "Initial user state")
    assert_success(get_catalog_by_isbn(ISBN), f"Initial catalog state for ISBN {ISBN}")

    # Check out
    assert_success(check_out(BOOK_IDS), "Check out books")
    assert_success(get_user(), "User state after checkout")
    assert_success(get_catalog_by_isbn(ISBN), "Catalog state after checkout")

    # Partial check-in
    assert_success(check_in([BOOK_IDS[0]]), "Partial check-in (1 book)")
    assert_success(get_user(), "User after partial check-in")
    assert_success(get_catalog_by_isbn(ISBN), "Catalog after partial check-in")

    # Final check-in
    assert_success(check_in([BOOK_IDS[1]]), "Final check-in (remaining book)")
    assert_success(get_user(), "Final user state")
    assert_success(get_catalog_by_isbn(ISBN), "Final catalog state")

    print("\n📋 Smoke Test Results Summary")
    all_passed = True
    for label, success in results:
        status = "✅" if success else "❌"
        print(f"{status} {label}")
        if not success:
            all_passed = False

    if not all_passed:
        print("\n❌ Smoke test failed")
        sys.exit(1)
    else:
        print("\n✅ All smoke tests passed")
        sys.exit(0)

if __name__ == "__main__":
    smoke_test()
