// Get URL parameters
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');

// Select DOM elements
const form = document.getElementById('resetForm');
const emailInput = document.getElementById('email');
const newPasswordInput = document.getElementById('newPassword');
const confirmPasswordInput = document.getElementById('confirmPassword');
const errorMessage = document.getElementById('error-message');
const successMessage = document.getElementById('success-message');

// Check if token is present in URL
if (!token) {
    errorMessage.textContent = 'Invalid or missing token.';
    errorMessage.style.display = 'block';
}

// Handle form submission
form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Basic validations
    if (!emailInput.value.trim()) {
        errorMessage.textContent = 'Email is required.';
        errorMessage.style.display = 'block';
        return;
    }

    if (newPasswordInput.value !== confirmPasswordInput.value) {
        errorMessage.textContent = 'Passwords do not match.';
        errorMessage.style.display = 'block';
        return;
    }

    // Prepare the request payload
    const resetRequest = {
        token: token,
        email: emailInput.value,
        newPassword: newPasswordInput.value
    };

    try {
        // Make the password reset request
        const response = await fetch('http://localhost:8080/api/authenticate/auth/reset-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(resetRequest)
        });

        const data = await response.json();

        if (response.ok) {
            successMessage.textContent = 'Your password has been reset successfully!';
            successMessage.style.display = 'block';
            errorMessage.style.display = 'none';
        } else {
            errorMessage.textContent = data.message || 'An error occurred. Please try again.';
            errorMessage.style.display = 'block';
            successMessage.style.display = 'none';
        }
    } catch (error) {
        console.log(error);
        errorMessage.textContent = 'Failed to reset password. Please try again later.';
        errorMessage.style.display = 'block';
        successMessage.style.display = 'none';
    }
});
